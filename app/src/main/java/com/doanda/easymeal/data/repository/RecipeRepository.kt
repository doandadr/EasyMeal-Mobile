package com.doanda.easymeal.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import com.doanda.easymeal.data.response.GeneralResponse
import com.doanda.easymeal.data.response.detailrecipe.DetailRecipeResponse
import com.doanda.easymeal.data.source.database.RecipeDao
import com.doanda.easymeal.data.source.model.RecipeEntity
import com.doanda.easymeal.data.source.remote.ApiService
import com.doanda.easymeal.data.source.remote.DummyApiService
import com.doanda.easymeal.utils.Result

class RecipeRepository(
    private val apiService: ApiService,
    private val dummyApiService: DummyApiService,
    private val recipeDao: RecipeDao,
) {

    fun getDetailRecipeById(recipeId: Int): LiveData<Result<DetailRecipeResponse>>
    = liveData {
        emit(Result.Loading)
        try {
//            val response = apiService.getDetailRecipeById(recipeId)
            val response = dummyApiService.getDetailRecipeById(recipeId)
            emit(Result.Success(response))
        } catch (e: Exception) {
            emit(Result.Error(e.message.toString()))
        }
    }

    fun getRecommendedRecipes(userId: Int): LiveData<Result<List<RecipeEntity>>>
    = liveData {
        emit(Result.Loading)
        try {
//            val response = apiService.getRecommendedRecipes(userId)
            val response = dummyApiService.getRecommendedRecipes(userId)

            val listRecipe = response.listRecipe
            val listRecipeId = mutableListOf<Int>()
            val listRecipeRoom = listRecipe.map { recipe ->
                val isFavorite = recipeDao.isRecipeFavorite(recipe.id)
                listRecipeId.add(recipe.id)
                RecipeEntity(
                    recipe.id,
                    recipe.title,
                    recipe.description,
                    recipe.totalTime,
                    recipe.serving,
                    recipe.imgUrl,
                    isFavorite = isFavorite,
                    isRecommended = true,
                )
            }
            recipeDao.resetRecommended()
            recipeDao.deleteRecipes(listRecipeId)
            recipeDao.insertRecipes(listRecipeRoom)
        } catch (e: Exception) {
            emit(Result.Error(e.message.toString()))
        }
        val localData: LiveData<Result<List<RecipeEntity>>> =
            recipeDao.getRecommendedRecipes().map { Result.Success(it) }
        emitSource(localData)
    }

    fun getFavoriteRecipes(userId: Int) : LiveData<Result<List<RecipeEntity>>>
    = liveData {
        emit(Result.Loading)
        try {
//            val response = apiService.getFavoriteRecipes(userId)
            val response = dummyApiService.getFavoriteRecipes(userId)

            val listRecipe = response.listFavoriteRecipe
            val listRecipeRoom = listRecipe.map { recipe ->
                val isRecommended = recipeDao.isRecipeRecommended(recipe.id)
                RecipeEntity(
                    recipe.id,
                    recipe.title,
                    recipe.description,
                    recipe.totalTime,
                    recipe.serving,
                    recipe.imgUrl,
                    isFavorite = true,
                    isRecommended = isRecommended,
                )
            }
            recipeDao.resetFavorite()
            recipeDao.insertReplaceRecipes(listRecipeRoom)
        } catch (e: Exception) {
            emit(Result.Error(e.message.toString()))
        }
        val localData: LiveData<Result<List<RecipeEntity>>> =
            recipeDao.getFavoriteRecipes().map { Result.Success(it) }
        emitSource(localData)
    }

    fun addFavoriteRecipe(userId: Int, recipeId: Int) : LiveData<Result<GeneralResponse>>
    = liveData {
        emit(Result.Loading)
        try {
//            val response = apiService.addFavoriteRecipe(userId, recipeId)
            val response = dummyApiService.addFavoriteRecipe(userId, recipeId)

            val recipe = recipeDao.getRecipeById(recipeId)
            recipe.isFavorite = true
            recipeDao.updateRecipe(recipe)

            emit(Result.Success(response))
        } catch (e: Exception) {
            emit(Result.Error(e.message.toString()))
        }
    }

    fun deleteFavoriteRecipe(userId: Int, recipeId: Int) : LiveData<Result<GeneralResponse>>
            = liveData {
        emit(Result.Loading)
        try {
//            val response = apiService.deleteFavoriteRecipe(userId, recipeId)
            val response = dummyApiService.deleteFavoriteRecipe(userId, recipeId)

            val recipe = recipeDao.getRecipeById(recipeId)
            recipe.isFavorite = false
            recipeDao.updateRecipe(recipe)

            emit(Result.Success(response))
        } catch (e: Exception) {
            emit(Result.Error(e.message.toString()))
        }
    }

    fun isRecipeFavoriteLocal(recipeId: Int) = recipeDao.isRecipeFavoriteObserve(recipeId)

    fun getFavoriteRecipesLocal() = recipeDao.getFavoriteRecipes()

    fun getRecommendedRecipesLocal() = recipeDao.getRecommendedRecipes()
    suspend fun clearFavorite() = recipeDao.resetFavorite()


    companion object {
        @Volatile
        private var INSTANCE: RecipeRepository? = null
        fun getInstance(
            apiService: ApiService,
            dummyApiService: DummyApiService,
            recipeDao: RecipeDao,
        ): RecipeRepository =
            INSTANCE?: synchronized(this) {
                INSTANCE?: RecipeRepository(apiService, dummyApiService, recipeDao)
            }.also { INSTANCE = it }
    }
}

