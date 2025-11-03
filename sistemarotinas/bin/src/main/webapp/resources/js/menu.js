function toggleSubMenu(element) {
    const submenu = element.nextElementSibling;
    if (submenu && submenu.classList.contains('submenu')) {
        submenu.classList.toggle('active');
    }
}