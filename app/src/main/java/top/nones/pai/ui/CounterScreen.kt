package top.nones.pai.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import top.nones.pai.ui.theme.DemoTheme

/**
 * ViewModel responsible for holding and managing the state of the CounterScreen.
 * It handles the business logic (increment/decrement).
 */
class CounterViewModel : ViewModel() {
    // The mutable state that holds the current count
    private val _count = MutableStateFlow(0)
    // The immutable state exposed to the UI
    val count: StateFlow<Int> = _count.asStateFlow()

    /**
     * Increments the counter by 1.
     */
    fun increment() {
        _count.update { it + 1 }
    }

    /**
     * Decrements the counter by 1.
     */
    fun decrement() {
        _count.update { it - 1 }
    }
}

/**
 * Composable function defining the actual UI for the counter.
 * It observes the state from the provided ViewModel.
 */
@Composable
fun CounterScreen(viewModel: CounterViewModel) {
    // Collect the count state from the ViewModel
    val count by viewModel.count.collectAsState()

    // Use Surface to provide the base Material 3 surface container
    Surface(
        modifier = Modifier
            .fillMaxSize(), // Make the screen take up the whole available space
        color = MaterialTheme.colorScheme.background // Use the theme's background color
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp), // Add padding around the whole screen content
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 1. Display the current count (Title/Text)
            Text(
                text = "$count",
                style = MaterialTheme.typography.displayLarge, // Use a large headline style
                color = MaterialTheme.colorScheme.primary // Use the primary color from the theme
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 2. Button Row (Increment and Decrement)
            Row(
                modifier = Modifier,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Decrement Button
                ElevatedButton(
                    onClick = { viewModel.decrement() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "-",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }

                // Increment Button
                ElevatedButton(
                    onClick = { viewModel.increment() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "+",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CounterScreenPreview() {
    // Preview the screen using a default/mocked ViewModel
    DemoTheme {
        CounterScreen(viewModel = CounterViewModel())
    }
}