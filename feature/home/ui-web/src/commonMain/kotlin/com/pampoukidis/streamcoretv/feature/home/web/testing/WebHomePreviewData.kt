package com.pampoukidis.streamcoretv.feature.home.web.testing

import com.pampoukidis.streamcoretv.core.model.content.RowModel
import com.pampoukidis.streamcoretv.core.ui.web.testing.WebBrowseFixtureScenario
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeUiState
import com.pampoukidis.streamcoretv.feature.home.common.testing.HomePreviewData

internal object WebHomePreviewData {
    private val backendFreeRows = HomePreviewData.rows.map { row ->
        row.copy(
            content = row.content.map { content ->
                content.copy(
                    poster = "",
                    backdrop = null,
                )
            },
        )
    }

    fun stateFor(scenario: WebBrowseFixtureScenario): HomeUiState {
        return when (scenario) {
            WebBrowseFixtureScenario.Loading -> HomeUiState()
            WebBrowseFixtureScenario.Content,
            WebBrowseFixtureScenario.Offline -> HomeUiState(
                isLoading = false,
                rows = backendFreeRows,
            )
            WebBrowseFixtureScenario.Empty,
            WebBrowseFixtureScenario.Error -> HomeUiState(isLoading = false)
            WebBrowseFixtureScenario.LongText -> HomeUiState(
                isLoading = false,
                rows = longTextRows(),
            )
        }
    }

    private fun longTextRows(): List<RowModel> {
        return backendFreeRows.mapIndexed { rowIndex, row ->
            row.copy(
                title = if (rowIndex == 0) {
                    "Επιλεγμένες ιστορίες για ατελείωτες βραδιές κινηματογράφου"
                } else {
                    row.title
                },
                subtitle = if (rowIndex == 0) {
                    "Ταινίες και σειρές που ταιριάζουν σε αυτή τη συλλογή"
                } else {
                    row.subtitle
                },
                content = row.content.mapIndexed { contentIndex, content ->
                    if (rowIndex == 0 && contentIndex == 0) {
                        content.copy(
                            title = "Η τελευταία αποστολή πέρα από τον ορατό ορίζοντα",
                            description = "Ένα πλήρωμα διασχίζει έναν άγνωστο κόσμο και ανακαλύπτει ότι η επιστροφή απαιτεί περισσότερα από θάρρος.",
                        )
                    } else {
                        content
                    }
                },
            )
        }
    }
}
