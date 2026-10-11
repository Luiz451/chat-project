const urlParams = new URLSearchParams(window.location.search);
const porta = urlParams.get('port') || "8080";
const ws = new WebSocket(`ws://localhost:${porta}`);

const messageInput = document.getElementById('message-input');
const sendButton = document.getElementById('send-button');
const chatForm = document.getElementById('chat-form');
const chatMessages = document.getElementById('chat-messages');
const currentChatTitle = document.getElementById('current-chat-title');
const connectionBadge = document.getElementById('connection-badge');
const connectionStatusDot = document.getElementById('connection-status-dot');

let contatoAtual = null;

ws.onopen = () => {
    connectionBadge.textContent = 'Conectado';
    connectionBadge.style.backgroundColor = '#10b981';
    connectionStatusDot.classList.replace('offline', 'online');

    const meuUser = prompt("Qual é o seu nome de usuário?");
    document.getElementById('my-username').textContent = meuUser;

    ws.send(`PEDIDO_ACESSO;${meuUser};123`);

    setTimeout(() => {
        contatoAtual = prompt("Com quem você quer falar?");
        currentChatTitle.textContent = contatoAtual;
        messageInput.disabled = false;
        sendButton.disabled = false;

        chatMessages.innerHTML = '';
        ws.send(`CARREGAR_HISTORICO;${contatoAtual}`);
    }, 1000);
};

ws.onmessage = (event) => {
    const dados = event.data.split('|', 2);
    if (dados.length === 2) {
        const remetente = dados[0];
        const texto = dados[1];

        const isSentByMe = (remetente === document.getElementById('my-username').textContent);
        adicionarMensagem(remetente, texto, isSentByMe);
    }
};

ws.onclose = () => {
    connectionBadge.textContent = 'Desconectado';
    connectionBadge.style.backgroundColor = '#ef4444';
    connectionStatusDot.classList.replace('online', 'offline');
};

chatForm.addEventListener('submit', (e) => {
    e.preventDefault();
    if (!contatoAtual || !messageInput.value.trim()) return;

    const texto = messageInput.value.trim();

    ws.send(`MENSAGEM;${contatoAtual};${texto}`);

    adicionarMensagem('Você', texto, true);
    messageInput.value = '';
});

function adicionarMensagem(remetente, texto, isSentByMe) {
    const row = document.createElement('div');
    row.className = `message-row ${isSentByMe ? 'sent' : 'received'}`;

    const nomeExibicao = isSentByMe ? 'Você' : remetente;
    const inicial = nomeExibicao.charAt(0).toUpperCase();

    row.innerHTML = `
        <div class="avatar ${isSentByMe ? 'me' : 'other'}">
            ${inicial}
        </div>
        <div class="message-content">
            <span class="message-info">${nomeExibicao}</span>
            <div class="bubble ${isSentByMe ? 'sent-bubble' : 'other-bubble'}">
                ${texto}
            </div>
        </div>
    `;

    chatMessages.appendChild(row);
    chatMessages.scrollTop = chatMessages.scrollHeight;
}