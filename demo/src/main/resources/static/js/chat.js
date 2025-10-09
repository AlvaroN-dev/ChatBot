// ========== CONFIGURACIÓN ==========
const CONFIG = {
    API_BASE_URL: 'http://localhost:8080/api/chat',
    MAX_RETRIES: 3,
    RETRY_DELAY: 1000
};

// ========== ESTADO DE LA APLICACIÓN ==========
const state = {
    sessionId: null,
    isConnected: false,
    isSending: false,
    messageHistory: []
};

// ========== ELEMENTOS DEL DOM ==========
const elements = {
    chatMessages: document.getElementById('chatMessages'),
    messageInput: document.getElementById('messageInput'),
    chatForm: document.getElementById('chatForm'),
    sendBtn: document.getElementById('sendBtn'),
    typingIndicator: document.getElementById('typingIndicator'),
    statusDot: document.getElementById('statusDot'),
    statusText: document.getElementById('statusText'),
    sessionIdDisplay: document.getElementById('sessionId'),
    charCount: document.getElementById('charCount'),
    newChatBtn: document.getElementById('newChatBtn'),
    clearHistoryBtn: document.getElementById('clearHistoryBtn'),
    toastContainer: document.getElementById('toastContainer')
};

// ========== INICIALIZACIÓN ==========
document.addEventListener('DOMContentLoaded', () => {
    initializeChat();
    attachEventListeners();
});

async function initializeChat() {
    try {
        // Crear nueva sesión
        await createNewSession();

        // Verificar health del servidor
        await checkHealth();

        updateStatus('connected', 'Conectado');
        showToast('Conexión establecida', 'success');
    } catch (error) {
        console.error('Error inicializando chat:', error);
        updateStatus('error', 'Error de conexión');
        showToast('No se pudo conectar al servidor', 'error');
    }
}

// ========== EVENT LISTENERS ==========
function attachEventListeners() {
    // Submit del formulario
    elements.chatForm.addEventListener('submit', handleSubmit);

    // Input de mensaje
    elements.messageInput.addEventListener('input', handleInput);
    elements.messageInput.addEventListener('keydown', handleKeyDown);

    // Botones de header
    elements.newChatBtn.addEventListener('click', handleNewChat);
    elements.clearHistoryBtn.addEventListener('click', handleClearHistory);

    // Quick actions
    document.querySelectorAll('.quick-action').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const message = e.currentTarget.dataset.message;
            elements.messageInput.value = message;
            handleInput();
            elements.messageInput.focus();
        });
    });
}

function handleInput() {
    const value = elements.messageInput.value;
    const length = value.length;

    // Actualizar contador
    elements.charCount.textContent = length;

    // Habilitar/deshabilitar botón de enviar
    elements.sendBtn.disabled = length === 0 || state.isSending;

    // Auto-resize del textarea
    elements.messageInput.style.height = 'auto';
    elements.messageInput.style.height = elements.messageInput.scrollHeight + 'px';
}

function handleKeyDown(e) {
    // Enviar con Enter (sin Shift)
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        if (!elements.sendBtn.disabled) {
            handleSubmit(e);
        }
    }
}

async function handleSubmit(e) {
    e.preventDefault();

    const message = elements.messageInput.value.trim();
    if (!message || state.isSending) return;

    // Limpiar welcome message si existe
    const welcomeMsg = elements.chatMessages.querySelector('.welcome-message');
    if (welcomeMsg) {
        welcomeMsg.remove();
    }

    // Mostrar mensaje del usuario
    addMessage('user', message);

    // Limpiar input
    elements.messageInput.value = '';
    handleInput();

    // Enviar mensaje
    await sendMessage(message);
}

async function handleNewChat() {
    const confirmed = confirm('¿Deseas iniciar una nueva conversación? Se perderá el historial actual.');
    if (!confirmed) return;

    try {
        await createNewSession();

        // Limpiar chat
        elements.chatMessages.innerHTML = '';
        elements.chatMessages.innerHTML = `
            <div class="welcome-message">
                <div class="welcome-icon">👋</div>
                <h2>¡Nueva conversación iniciada!</h2>
                <p>¿En qué puedo ayudarte?</p>
            </div>
        `;

        state.messageHistory = [];
        showToast('Nueva conversación creada', 'success');
    } catch (error) {
        showToast('Error al crear nueva conversación', 'error');
    }
}

async function handleClearHistory() {
    if (!state.sessionId) {
        showToast('No hay sesión activa', 'info');
        return;
    }

    const confirmed = confirm('¿Deseas limpiar el historial de esta conversación?');
    if (!confirmed) return;

    try {
        await clearHistory(state.sessionId);

        // Limpiar UI
        elements.chatMessages.innerHTML = `
            <div class="welcome-message">
                <div class="welcome-icon">🗑️</div>
                <h2>Historial limpiado</h2>
                <p>El historial de la conversación ha sido eliminado.</p>
            </div>
        `;

        state.messageHistory = [];
        showToast('Historial limpiado exitosamente', 'success');
    } catch (error) {
        showToast('Error al limpiar historial', 'error');
    }
}

// ========== API CALLS ==========
async function sendMessage(message) {
    state.isSending = true;
    elements.sendBtn.disabled = true;
    showTypingIndicator();

    try {
        const response = await fetch(`${CONFIG.API_BASE_URL}/message`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                message: message,
                sessionId: state.sessionId
            })
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        const data = await response.json();

        if (data.success) {
            addMessage('assistant', data.message);
            state.messageHistory.push({
                user: message,
                assistant: data.message,
                timestamp: new Date()
            });
        } else {
            throw new Error(data.error || 'Error desconocido');
        }

    } catch (error) {
        console.error('Error enviando mensaje:', error);
        addMessage('error', `Error: ${error.message}`);
        showToast('Error al enviar mensaje', 'error');
    } finally {
        hideTypingIndicator();
        state.isSending = false;
        elements.sendBtn.disabled = false;
        elements.messageInput.focus();
    }
}

async function createNewSession() {
    try {
        const response = await fetch(`${CONFIG.API_BASE_URL}/session/new`);
        const data = await response.json();

        if (data.success) {
            state.sessionId = data.sessionId;
            elements.sessionIdDisplay.textContent = data.sessionId.substring(0, 8);
            return data.sessionId;
        }

        throw new Error('No se pudo crear sesión');
    } catch (error) {
        console.error('Error creando sesión:', error);
        throw error;
    }
}

async function clearHistory(sessionId) {
    try {
        const response = await fetch(`${CONFIG.API_BASE_URL}/history/${sessionId}`, {
            method: 'DELETE'
        });

        const data = await response.json();

        if (!data.success) {
            throw new Error(data.error || 'Error al limpiar historial');
        }

        return data;
    } catch (error) {
        console.error('Error limpiando historial:', error);
        throw error;
    }
}

async function checkHealth() {
    try {
        const response = await fetch(`${CONFIG.API_BASE_URL}/health`);
        const data = await response.json();

        state.isConnected = data.status === 'UP';
        return data;
    } catch (error) {
        console.error('Error en health check:', error);
        state.isConnected = false;
        throw error;
    }
}

// ========== UI FUNCTIONS ==========
function addMessage(type, content) {
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${type}`;

    const timestamp = new Date().toLocaleTimeString('es-ES', {
        hour: '2-digit',
        minute: '2-digit'
    });

    let roleLabel = '';
    if (type === 'user') roleLabel = 'Tú';
    else if (type === 'assistant') roleLabel = '🤖 Asistente';
    else if (type === 'error') roleLabel = '⚠️ Error';

    messageDiv.innerHTML = `
        <div class="message-content">
            <div class="message-header">
                <strong>${roleLabel}</strong>
            </div>
            <div class="message-text">${escapeHtml(content)}</div>
            <div class="message-footer">${timestamp}</div>
        </div>
    `;

    elements.chatMessages.appendChild(messageDiv);
    scrollToBottom();
}

function showTypingIndicator() {
    elements.typingIndicator.style.display = 'flex';
    scrollToBottom();
}

function hideTypingIndicator() {
    elements.typingIndicator.style.display = 'none';
}

function scrollToBottom() {
    setTimeout(() => {
        elements.chatMessages.scrollTop = elements.chatMessages.scrollHeight;
    }, 100);
}

function updateStatus(status, text) {
    elements.statusDot.className = `status-dot ${status}`;
    elements.statusText.textContent = text;
    state.isConnected = status === 'connected';
}

function showToast(message, type = 'info') {
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;

    const icons = {
        success: '✓',
        error: '✕',
        info: 'ℹ'
    };

    toast.innerHTML = `
        <span style="font-size: 1.25rem;">${icons[type]}</span>
        <span>${message}</span>
    `;

    elements.toastContainer.appendChild(toast);

    // Auto-remove después de 3 segundos
    setTimeout(() => {
        toast.style.animation = 'toastSlide 0.3s ease-out reverse';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

// ========== UTILIDADES ==========
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// ========== MANEJO DE ERRORES GLOBAL ==========
window.addEventListener('error', (event) => {
    console.error('Error global:', event.error);
    showToast('Ocurrió un error inesperado', 'error');
});

window.addEventListener('unhandledrejection', (event) => {
    console.error('Promise rechazada:', event.reason);
    showToast('Error en la operación', 'error');
});

// ========== RECONEXIÓN AUTOMÁTICA ==========
let reconnectInterval = null;

function startReconnection() {
    if (reconnectInterval) return;

    updateStatus('connecting', 'Reconectando...');

    reconnectInterval = setInterval(async () => {
        try {
            await checkHealth();

            if (state.isConnected) {
                updateStatus('connected', 'Conectado');
                showToast('Conexión restablecida', 'success');
                stopReconnection();
            }
        } catch (error) {
            console.log('Intento de reconexión fallido');
        }
    }, 5000);
}

function stopReconnection() {
    if (reconnectInterval) {
        clearInterval(reconnectInterval);
        reconnectInterval = null;
    }
}

// ========== DETECTAR PÉRDIDA DE CONEXIÓN ==========
window.addEventListener('online', () => {
    showToast('Conexión a internet restaurada', 'success');
    checkHealth().catch(() => startReconnection());
});

window.addEventListener('offline', () => {
    updateStatus('error', 'Sin conexión');
    showToast('Sin conexión a internet', 'error');
});

// ========== FUNCIONES ADICIONALES ==========

/**
 * Obtiene el historial de la conversación actual
 */
async function getHistory() {
    if (!state.sessionId) {
        showToast('No hay sesión activa', 'info');
        return null;
    }

    try {
        const response = await fetch(`${CONFIG.API_BASE_URL}/history/${state.sessionId}`);
        const data = await response.json();

        if (data.success) {
            return data.messages;
        }

        throw new Error(data.error || 'Error al obtener historial');
    } catch (error) {
        console.error('Error obteniendo historial:', error);
        showToast('Error al obtener historial', 'error');
        return null;
    }
}

/**
 * Guarda el historial en localStorage (opcional)
 */
function saveHistoryToLocal() {
    try {
        const historyData = {
            sessionId: state.sessionId,
            messages: state.messageHistory,
            timestamp: new Date().toISOString()
        };

        localStorage.setItem('chatHistory', JSON.stringify(historyData));
        showToast('Historial guardado localmente', 'success');
    } catch (error) {
        console.error('Error guardando historial:', error);
        showToast('Error al guardar historial', 'error');
    }
}

/**
 * Carga el historial desde localStorage (opcional)
 */
function loadHistoryFromLocal() {
    try {
        const historyData = localStorage.getItem('chatHistory');

        if (historyData) {
            const data = JSON.parse(historyData);

            // Verificar que no sea muy antiguo (24 horas)
            const savedTime = new Date(data.timestamp);
            const now = new Date();
            const hoursDiff = (now - savedTime) / (1000 * 60 * 60);

            if (hoursDiff < 24) {
                state.sessionId = data.sessionId;
                state.messageHistory = data.messages;

                // Restaurar mensajes en UI
                elements.chatMessages.innerHTML = '';
                data.messages.forEach(msg => {
                    addMessage('user', msg.user);
                    addMessage('assistant', msg.assistant);
                });

                showToast('Historial restaurado', 'info');
            }
        }
    } catch (error) {
        console.error('Error cargando historial:', error);
    }
}

/**
 * Exporta la conversación como texto
 */
function exportConversation() {
    if (state.messageHistory.length === 0) {
        showToast('No hay mensajes para exportar', 'info');
        return;
    }

    let text = `Conversación - Sesión: ${state.sessionId}\n`;
    text += `Fecha: ${new Date().toLocaleString('es-ES')}\n`;
    text += `${'='.repeat(50)}\n\n`;

    state.messageHistory.forEach((msg, index) => {
        text += `Mensaje ${index + 1}:\n`;
        text += `Usuario: ${msg.user}\n`;
        text += `Asistente: ${msg.assistant}\n`;
        text += `Hora: ${new Date(msg.timestamp).toLocaleString('es-ES')}\n`;
        text += `${'-'.repeat(50)}\n\n`;
    });

    // Crear y descargar archivo
    const blob = new Blob([text], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `conversacion_${state.sessionId.substring(0, 8)}.txt`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);

    showToast('Conversación exportada', 'success');
}

/**
 * Limpia mensajes antiguos del UI (mantiene últimos N)
 */
function cleanupOldMessages(keepLast = 50) {
    const messages = elements.chatMessages.querySelectorAll('.message');

    if (messages.length > keepLast) {
        const toRemove = messages.length - keepLast;

        for (let i = 0; i < toRemove; i++) {
            messages[i].remove();
        }

        showToast(`${toRemove} mensajes antiguos eliminados`, 'info');
    }
}

// ========== ATAJOS DE TECLADO ==========
document.addEventListener('keydown', (e) => {
    // Ctrl/Cmd + K: Nueva conversación
    if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
        e.preventDefault();
        handleNewChat();
    }

    // Ctrl/Cmd + L: Limpiar historial
    if ((e.ctrlKey || e.metaKey) && e.key === 'l') {
        e.preventDefault();
        handleClearHistory();
    }

    // Ctrl/Cmd + S: Guardar historial
    if ((e.ctrlKey || e.metaKey) && e.key === 's') {
        e.preventDefault();
        saveHistoryToLocal();
    }

    // Ctrl/Cmd + E: Exportar conversación
    if ((e.ctrlKey || e.metaKey) && e.key === 'e') {
        e.preventDefault();
        exportConversation();
    }
});

// ========== HEALTH CHECK PERIÓDICO ==========
setInterval(async () => {
    try {
        await checkHealth();

        if (!state.isConnected) {
            updateStatus('error', 'Desconectado');
            startReconnection();
        }
    } catch (error) {
        if (state.isConnected) {
            updateStatus('error', 'Error de conexión');
            startReconnection();
        }
    }
}, 30000); // Cada 30 segundos

// ========== MENSAJES DE CONSOLA ==========
console.log('%c🤖 Riwi ChatBot v1.0', 'font-size: 20px; color: #667eea; font-weight: bold;');
console.log('%cAtajos de teclado disponibles:', 'color: #48bb78; font-weight: bold;');
console.log('  Ctrl/Cmd + K: Nueva conversación');
console.log('  Ctrl/Cmd + L: Limpiar historial');
console.log('  Ctrl/Cmd + S: Guardar historial localmente');
console.log('  Ctrl/Cmd + E: Exportar conversación');

// ========== EXPONER FUNCIONES ÚTILES A LA CONSOLA (DEV) ==========
if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
    window.chatDebug = {
        getState: () => state,
        getHistory: getHistory,
        exportConversation: exportConversation,
        saveHistory: saveHistoryToLocal,
        loadHistory: loadHistoryFromLocal,
        clearMessages: () => {
            elements.chatMessages.innerHTML = '';
            showToast('UI limpiada', 'info');
        },
        testToast: (msg, type) => showToast(msg, type)
    };

    console.log('%c💡 Modo desarrollo activado', 'color: #fbbf24; font-weight: bold;');
    console.log('Usa window.chatDebug para funciones de debugging');
}