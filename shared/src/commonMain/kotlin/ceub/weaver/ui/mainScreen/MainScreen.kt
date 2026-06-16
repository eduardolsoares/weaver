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
import ceub.weaver.ui.mainScreen.components.* // 🟢 Importa todos os seus novos componentes limpos
import ceub.weaver.domain.model.ProjectResponse
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    onNewProjectClick: () -> Unit,
    onProjectClick: (projectName: String) -> Unit,
    userEmail: String = "arthur@email.com",
    onFetchProjects: suspend (String) -> List<ProjectResponse>,
    onCreateProject: suspend (String, String) -> ProjectResponse?,
    onDeleteProject: suspend (String) -> Boolean,
    onRenameProject: suspend (String, String) -> Boolean
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

    LaunchedEffect(userEmail) {
        isLoading = true
        projects = onFetchProjects(userEmail)
        isLoading = false
    }

    // 🟢 Componente isolado de Alerta
    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            project = projectToConfirmDelete,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                projectToConfirmDelete?.id?.let { projectId ->
                    scope.launch {
                        if (onDeleteProject(projectId)) {
                            projects = onFetchProjects(userEmail)
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
                renameErrorMessage = null // Limpa o erro ao fechar
            },
            containerColor = Color(0xFF1E293B), // Slate-800
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
                            // 🟢 Limpa o aviso de erro assim que o usuário volta a digitar
                            if (renameErrorMessage != null) renameErrorMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isError = renameErrorMessage != null, // 🟢 Deixa a borda vermelha se houver erro
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0A0C0F),
                            unfocusedContainerColor = Color(0xFF0A0C0F),
                            focusedTextColor = Color(0xFFF1F5F9),
                            unfocusedTextColor = Color(0xFFF1F5F9),
                            cursorColor = Color(0xFF818CF8),
                            errorContainerColor = Color(0xFF0A0C0F) // Mantém o fundo escuro no erro
                        )
                    )

                    // 🟢 Exibe o aviso de erro logo abaixo do TextField se ele existir
                    if (renameErrorMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = renameErrorMessage!!,
                            color = Color(0xFFEF4444), // Vermelho do Tailwind
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // 🟢 Validações ao tentar Salvar:
                        when {
                            newNameInput.isBlank() -> {
                                renameErrorMessage = "O nome do projeto não pode ser vazio."
                            }
                            !newNameInput.matches(namePattern) -> {
                                renameErrorMessage = "Caracteres especiais ou emojis não são permitidos."
                            }
                            else -> {
                                // Se passou nas duas travas, executa a requisição
                                showRenameDialog = false
                                renameErrorMessage = null
                                projectToRename?.id?.let { projectId ->
                                    scope.launch {
                                        if (onRenameProject(projectId, newNameInput)) {
                                            projects = onFetchProjects(userEmail)
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
                        renameErrorMessage = null // Limpa o erro ao cancelar
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

            // 🟢 Seção de criação limpa e modular
            item {
                NewProjectSection(onCardClick = {
                    scope.launch {
                        val newProject = onCreateProject("Projeto sem nome", userEmail)
                        if (newProject != null) {
                            projects = onFetchProjects(userEmail)
                            onNewProjectClick()
                        }
                    }
                })
            }

            // Section: Recent modelings
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