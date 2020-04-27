package dev.marcosfarias.pokedex.ui.pokedex

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import dev.marcosfarias.pokedex.database.dao.PokemonDAO
import dev.marcosfarias.pokedex.model.Pokemon
import dev.marcosfarias.pokedex.repository.PokemonService
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

class PokedexRepository(
    private val pokemonDAO: PokemonDAO,
    private val pokemonService: PokemonService
) {
    private var loadingLock = false
    private var pokedexList = mutableListOf<Pokemon>()

    val loadingData = MutableLiveData(false)
    val pokedexListData = MutableLiveData<MutableList<Pokemon>>()

    suspend fun loadPokedexList(offset: String, limit: String) {
        if (!loadingLock) {
            loadingLock = true
            loadingData.postValue(loadingLock)
            val cache = pokemonDAO.getByRangeOffsetId(offset)
            if (cache.isNotEmpty()) {
                val data = pokemonDAO.all()
                pokedexListData.postValue(data)
                loadingLock = false
                loadingData.postValue(loadingLock)
            } else {
                getPokedexList(offset, limit)
                    .subscribeOn(Schedulers.io())
                    .subscribe({ pokedexItem ->
                        pokedexList.add(pokedexItem)
                        pokemonDAO.add(pokedexItem)
                    }, { error ->
                        error.printStackTrace()
                        loadingLock = false
                        loadingData.postValue(loadingLock)
                    }, {
                        pokedexListData.postValue(pokedexList)
                        loadingLock = false
                        loadingData.postValue(loadingLock)
                    })
            }
        }
    }

    private fun getPokedexList(
        offset: String,
        limit: String): Observable<Pokemon> {
        return pokemonService.getNextList(offset, limit)
            .flatMap { pokemonResults ->
                Observable.fromIterable(pokemonResults.results)
            }.flatMap { pokemonName ->
                pokemonService.getById(Uri.parse(pokemonName.url).lastPathSegment.toString())
            }.map { pokemon ->
                Pokemon(
                    pokemon.id,
                    pokemon.name,
                    pokemon.baseExperience,
                    pokemon.height,
                    pokemon.isDefault,
                    pokemon.order,
                    pokemon.weight,
                    pokemon.species,
                    pokemon.abilities,
                    pokemon.forms,
                    pokemon.gameIndices,
                    pokemon.heldItems,
                    pokemon.moves,
                    pokemon.stats,
                    pokemon.types,
                    pokemon.sprites
                )
            }
    }
}