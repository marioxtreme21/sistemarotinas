function toggleSidebarMeusChamados() {
    // Obtém elementos do DOM
    const sidebar = document.getElementById('meusChamadosSidebar');
    const mainContent = document.getElementById('mainContent');
    const icon = document.getElementById('toggleIcon');

    // Se algum elemento não for encontrado, registra aviso e aborta função
    if (!sidebar || !mainContent || !icon) {
        console.warn('Elementos sidebar, mainContent ou icon não encontrados.');
        return;
    }

    // Verifica se a sidebar está colapsada (fechada)
    const isCollapsed = sidebar.classList.contains('collapsed');

    if (isCollapsed) {
        // Abre a sidebar removendo classe 'collapsed'
        sidebar.classList.remove('collapsed');
        mainContent.classList.remove('margin-collapsed');
        mainContent.classList.add('margin-expanded');

        // Muda o ícone para "chevron-left" indicando que pode fechar
        icon.classList.remove('pi-chevron-right');
        icon.classList.add('pi-chevron-left');

        // Salva estado no localStorage para persistir entre recarregamentos
        localStorage.setItem('sidebarCollapsed', 'false');
    } else {
        // Fecha a sidebar adicionando classe 'collapsed'
        sidebar.classList.add('collapsed');
        mainContent.classList.remove('margin-expanded');
        mainContent.classList.add('margin-collapsed');

        // Muda o ícone para "chevron-right" indicando que pode abrir
        icon.classList.remove('pi-chevron-left');
        icon.classList.add('pi-chevron-right');

        // Salva estado no localStorage
        localStorage.setItem('sidebarCollapsed', 'true');
    }
}

// Função que aplica o estado salvo no carregamento da página
function aplicarEstadoSidebar() {
    const sidebar = document.getElementById('meusChamadosSidebar');
    const mainContent = document.getElementById('mainContent');
    const icon = document.getElementById('toggleIcon');
    const estado = localStorage.getItem('sidebarCollapsed');

    if (!sidebar || !mainContent || !icon) return;

    if (estado === 'true') {
        // Aplica estado fechado se salvo
        sidebar.classList.add('collapsed');
        mainContent.classList.remove('margin-expanded');
        mainContent.classList.add('margin-collapsed');

        icon.classList.remove('pi-chevron-left');
        icon.classList.add('pi-chevron-right');
    } else {
        // Aplica estado aberto
        sidebar.classList.remove('collapsed');
        mainContent.classList.remove('margin-collapsed');
        mainContent.classList.add('margin-expanded');

        icon.classList.remove('pi-chevron-right');
        icon.classList.add('pi-chevron-left');
    }
}

// Aplica o estado ao carregar o documento
document.addEventListener('DOMContentLoaded', aplicarEstadoSidebar);
