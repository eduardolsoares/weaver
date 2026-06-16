package ceub.weaver.ui.mainScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ceub.weaver.ui.mainScreen.components.NewProjectCard
import ceub.weaver.ui.mainScreen.components.RecentProjectCard
import ceub.weaver.ui.mainScreen.components.SearchBar
import ceub.weaver.domain.model.ProjectResponse
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    onNewProjectClick: () -> Unit,
    onProjectClick: (projectName: String) -> Unit,
    userEmail: String = "arthur@email.com",
    onFetchProjects: suspend (String) -> List<ProjectResponse>,
    onCreateProject: suspend (String, String) -> ProjectResponse?,
    onDeleteProject: suspend (String) -> Boolean
) {
    val scope = rememberCoroutineScope()
    var projects by remember { mutableStateOf<List<ProjectResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userEmail) {
        isLoading = true
        projects = onFetchProjects(userEmail)
        isLoading = false
    }

    Scaffold(
        topBar = { SearchBar() },
        containerColor = Color(0xFF0A0C0F)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0C0F))
                .padding(padding)
        ) {
            item {
                Surface(
                    color = Color(0xFF0A0C0F),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(
                            modifier = Modifier
                                .padding(vertical = 32.dp)
                                .widthIn(max = 1000.dp)
                                .fillMaxWidth()
                                .padding(horizontal = 40.dp)
                        ) {
                            Text(
                                text = "Começar um novo projeto",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFF1F5F9)
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            NewProjectCard(onClick = {
                                scope.launch {
                                    val newProject = onCreateProject("Projeto sem nome", userEmail)
                                    if (newProject != null) {
                                        projects = onFetchProjects(userEmail)
                                        onNewProjectClick()
                                    }
                                }
                            })
                        }
                    }
                }
            }

            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(
                        modifier = Modifier
                            .padding(vertical = 32.dp)
                            .widthIn(max = 1000.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 40.dp)
                    ) {
                        Text(
                            text = "Meus Projetos",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFF1F5F9)
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        when {
                            isLoading -> {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = Color(0xFF3B82F6))
                                }
                            }
                            projects.isEmpty() -> {
                                Text(
                                    text = "Nenhum projeto encontrado",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF64748B)
                                )
                            }
                            else -> {
                                RealProjectsGrid(
                                    projectsList = projects,
                                    onProjectClick = { project -> onProjectClick(project.name) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RealProjectsGrid(
    projectsList: List<ProjectResponse>,
    onProjectClick: (ProjectResponse) -> Unit
) {
    projectsList.chunked(4).forEach { rowItems ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            rowItems.forEach { project ->
                RecentProjectCard(
                    title = project.name,
                    lastModified = "Modificado em: ${project.updatedAt.take(10)}",
                    onClick = { onProjectClick(project) }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}