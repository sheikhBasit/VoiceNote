package com.example.voicenote.features.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicenote.data.repository.VoiceNoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchResult(
    val query: String,
    val answer: String,
    val source: String,
    val localResults: List<String> = emptyList(),
    val webResults: List<String> = emptyList()
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: VoiceNoteRepository
) : ViewModel() {
    var searchResult by mutableStateOf<SearchResult?>(null)
    var isSearching by mutableStateOf(false)

    fun performSearch(query: String) {
        if (query.isBlank()) return
        isSearching = true
        viewModelScope.launch {
            repository.searchNotes(query).collect { result ->
                result.onSuccess { data ->
                    searchResult = SearchResult(
                        query = data.query,
                        answer = data.answer,
                        source = data.source,
                        localResults = data.results.map { it.summary }
                    )
                }.onFailure { e ->
                    searchResult = SearchResult(query, "Error searching: ${e.message}", "System")
                }
                isSearching = false
            }
        }
    }
}
