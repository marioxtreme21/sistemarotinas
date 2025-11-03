let videoStream;

function startCamera() {
    console.log("✅ startCamera() foi chamado");

    const video = document.getElementById('video');
    if (!video) {
        console.warn("⚠️ Elemento <video> não encontrado no DOM.");
        return;
    }

    navigator.mediaDevices.getUserMedia({
        video: {
            facingMode: 'environment',
            width: { ideal: 1920 },
            height: { ideal: 1080 }
        }
    })
    .then(stream => {
        videoStream = stream;
        video.srcObject = stream;
        console.log("🎥 Câmera ativada com sucesso.");
    })
    .catch(err => {
        console.error("❌ Erro ao acessar webcam:", err);
        alert("Erro ao acessar webcam: " + err.message);
    });
}

function captureImage() {
    console.log("📸 Botão 'Capturar' clicado.");

    const video = document.getElementById('video');
    const overlay = video.nextElementSibling;

    if (!video || !overlay) {
        console.warn("⚠️ Elementos de vídeo ou moldura não encontrados.");
        return;
    }

    const videoRect = video.getBoundingClientRect();
    const overlayRect = overlay.getBoundingClientRect();

    const realWidth = video.videoWidth;
    const realHeight = video.videoHeight;
    const renderWidth = video.offsetWidth;
    const renderHeight = video.offsetHeight;

    const scaleX = realWidth / renderWidth;
    const scaleY = realHeight / renderHeight;

    const offsetX = overlayRect.left - videoRect.left;
    const offsetY = overlayRect.top - videoRect.top;

    const ajusteVerticalInicio = -50; // Sobe um pouco o início da captura
    const ajusteVerticalFinal = -100;  // Encolhe um pouco a altura total

    const cropX = offsetX * scaleX;
    const cropY = (offsetY + ajusteVerticalInicio) * scaleY;
    const cropW = overlay.offsetWidth * scaleX;
    const cropH = (overlay.offsetHeight - ajusteVerticalFinal) * scaleY;

    console.log("📐 Área real recortada:", {
        cropX: cropX.toFixed(2),
        cropY: cropY.toFixed(2),
        cropW: cropW.toFixed(2),
        cropH: cropH.toFixed(2)
    });

    const canvas = document.createElement('canvas');
    canvas.width = cropW;
    canvas.height = cropH;

    const ctx = canvas.getContext('2d');
    ctx.drawImage(video, cropX, cropY, cropW, cropH, 0, 0, canvas.width, canvas.height);

    const base64 = canvas.toDataURL('image/png');

    const hiddenInput = document.getElementById('formCaptura:imageBase64');
    if (hiddenInput) {
        hiddenInput.value = base64;
        console.log("📝 Base64 setado no input.");
    } else {
        console.warn("⚠️ Campo imageBase64 não encontrado.");
    }

    const preview = document.getElementById('formCaptura:preview');
    if (preview) {
        preview.src = base64;
        console.log("🖼️ Preview atualizado.");
    }
}

function ativarCameraDepoisAjax() {
    console.log("🔄 Chamada pós-AJAX: ativando câmera...");
    setTimeout(startCamera, 500);
}

window.onbeforeunload = function () {
    if (videoStream) {
        videoStream.getTracks().forEach(track => track.stop());
        console.log("🛑 Câmera desligada ao sair da página.");
    }
};

window.ativarCameraDepoisAjax = ativarCameraDepoisAjax;
