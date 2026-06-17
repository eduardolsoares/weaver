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
import ceub.weaver.ui.mainScreen.components.*
import ceub.weaver.domain.model.ProjectResponse
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    onNewProjectClick: () -> Unit,
    onProjectClick: (projectName: String) -> Unit,
    idToken: String,
    onFetchProjects: suspend (String) -> List<ProjectResponse>,
    onCreateProject: suspend (String, String) -> ProjectResponse?,
    onDeleteProject: suspend (String, String) -> Boolean,
    onRenameProject: suspend (String, String, String) -> Boolean
) {
    val scope = rememberCoroutineScope()
    val namePattern = remember { Regex("^[a-zA-Z0-9 áàâãéèêíïóôõöúçñÁÀÂÃÉÈÊÍÏÓÔÕÖÚÇÑ]*$") }
    var projects by remember { mutableStateOf<List<ProjectResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var projectToConfirmDelete by remember { mutableStateOf<ProjectResponse?>(null) }

    var showRenameDialog by remember { mutableStateOf(false) }
    var projectToRename by remember { mutableStateOf<ProjectResponse?>(null) }
    var newNameInput by remember { mutableStateOf("") }
    var renameErrorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(idToken) {
        isLoading = true
        projects = onFetchProjects(idToken)
        isLoading = false
    }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            project = projectToConfirmDelete,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                projectToConfirmDelete?.id?.let { projectId ->
                    scope.launch {
                        if (onDeleteProject(projectId, idToken)) {
                            projects = onFetchProjects(idToken)
                        }
                    }
                }
            }
        )
    }

    if (showRenameDialog && projectToRename != null) {
        AlertDialog(
            onDismissRequest = {
                showRenameDialog = false
                renameErrorMessage = null
            },
            containerColor = Color(0xFF1E293B),
            title = {
                Text("Renomear Projeto", color = Color(0xFFF1F5F9), style = MaterialTheme.typography.titleMedium)
            },
            text = {
                Column {
                    Text("Digite o novo nome para o projeto:", color = Color(0xFF94A3B8), modifier = Modifier.padding(bottom = 12.dp))

                    TextField(
                        value = newNameInput,
                        onValueChange = {
                            newNameInput = it
                            if (renameErrorMessage != null) renameErrorMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isError = renameErrorMessage != null,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0A0C0F),
                            unfocusedContainerColor = Color(0xFF0A0C0F),
                            focusedTextColor = Color(0xFFF1F5F9),
                            unfocusedTextColor = Color(0xFFF1F5F9),
                            cursorColor = Color(0xFF818CF8),
                            errorContainerColor = Color(0xFF0A0C0F)
                        )
                    )

                    if (renameErrorMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = renameErrorMessage!!,
                            color = Color(0xFFEF4444),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        when {
                            newNameInput.isBlank() -> {
                                renameErrorMessage = "O nome do projeto não pode ser vazio."
                            }
                            !newNameInput.matches(namePattern) -> {
                                renameErrorMessage = "Caracteres especiais não são permitidos."
                            }
                            else -> {
                                showRenameDialog = false
                                renameErrorMessage = null
                                projectToRename?.id?.let { projectId ->
                                    scope.launch {
                                        if (onRenameProject(projectId, newNameInput, idToken)) {
                                            projects = onFetchProjects(idToken)
                                        }
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Text("Salvar", color = Color(0xFF818CF8))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRenameDialog = false
                        renameErrorMessage = null
                    }
                ) {
                    Text("Cancelar", color = Color(0xFF94A3B8))
                }
            }
        )
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
                NewProjectSection(onCardClick = {
                    scope.launch {
                        val newProject = onCreateProject("Projeto sem nome", idToken)
                        if (newProject != null) {
                            projects = onFetchProjects(idToken)
                            onNewProjectClick()
                        }
                    }
                })
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
                                    onProjectClick = { project -> onProjectClick(project.name) },
                                    onDeleteProjectClick = { selectedProject ->
                                        projectToConfirmDelete = selectedProject
                                        showDeleteDialog = true
                                    },
                                    onRenameProjectClick = { selectedProject ->
                                        projectToRename = selectedProject
                                        newNameInput = selectedProject.name
                                        showRenameDialog = true
                                    }
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
    onProjectClick: (ProjectResponse) -> Unit,
    onDeleteProjectClick: (ProjectResponse) -> Unit,
    onRenameProjectClick: (ProjectResponse) -> Unit
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
                    onClick = { onProjectClick(project) },
                    onDeleteClick = { onDeleteProjectClick(project) },
                    onRenameClick = { onRenameProjectClick(project) }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}