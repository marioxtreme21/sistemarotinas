console.log('enter-navigation.js carregado com sucesso');

document.addEventListener('keydown', function (event) {
    const target = event.target;

    if (event.key === 'Enter' && target.tagName !== 'TEXTAREA' && target.type !== 'submit') {
        event.preventDefault();

        const focusables = Array.from(document.querySelectorAll(
            'input:not([type=hidden]):not([disabled]), select, textarea, button, [tabindex]:not([tabindex="-1"])'
        )).filter(el => el.offsetParent !== null); // visíveis

        const index = focusables.indexOf(target);
        if (index > -1 && index + 1 < focusables.length) {
            focusables[index + 1].focus();
        }
    }
});