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
const usersList = document.getElementById('users-list');
const addContactBtn = document.getElementById('add-contact-btn'); // NOVO

let contatoAtual = null;
let digitandoTimeout = null;
let listaContatosLocal = new Map(); // NOVO: "Cérebro" local dos contatos

ws.onopen = () => {
    connectionBadge.textContent = 'Conectado';
    connectionBadge.style.backgroundColor = '#10b981';
    connectionStatusDot.classList.replace('offline', 'online');

    let inputUser = prompt("Qual é o seu nome de usuário?");
    if (!inputUser) inputUser = "anonimo";

    const meuUser = inputUser.trim().toLowerCase();
    document.getElementById('my-username').textContent = meuUser;

    ws.send(`PEDIDO_ACESSO;${meuUser};123`);
};

ws.onmessage = (event) => {
    const dados = event.data.split('|', 3);
    if (dados.length >= 3) {
        const evento = dados[0];
        const remetente = dados[1];
        const texto = dados[2];

        if (evento === 'RESPOSTA_ACESSO' && texto.includes('Falha')) {
            const meuUser = document.getElementById('my-username').textContent;
            ws.send(`REGISTRO;${meuUser};123`);
            setTimeout(() => ws.send(`PEDIDO_ACESSO;${meuUser};123`), 300);
        }
        else if (evento === 'ENTREGA_MENSAGEM') {
            const isSentByMe = (remetente === document.getElementById('my-username').textContent);

            // Se alguém desconhecido enviar mensagem, adiciona à lista como ONLINE
            if (!isSentByMe && !listaContatosLocal.has(remetente)) {
                listaContatosLocal.set(remetente, 'ONLINE');
                atualizarInterfaceContatos();
            }

            if (isSentByMe || remetente === contatoAtual) {
                adicionarMensagem(remetente, texto, isSentByMe);
            }
        }
        else if (evento === 'LISTA_CONTATOS') {
            processarListaServidor(texto);
        }
        else if (evento === 'INICIO_DIGITACAO') {
            if (remetente === contatoAtual) {
                document.getElementById('current-recipient-badge').textContent = 'digitando...';
            }
        }
        else if (evento === 'FIM_DIGITACAO') {
            if (remetente === contatoAtual) {
                const status = listaContatosLocal.get(contatoAtual) || 'OFFLINE';
                document.getElementById('current-recipient-badge').textContent = status === 'ONLINE' ? 'Online' : 'Offline';
            }
        }
    }
};

ws.onclose = () => {
    connectionBadge.textContent = 'Desconectado';
    connectionBadge.style.backgroundColor = '#ef4444';
    connectionStatusDot.classList.replace('online', 'offline');
};

// --- NOVO: Lógica de Adicionar Contato Manualmente ---
addContactBtn.addEventListener('click', () => {
    let novo = prompt("Com quem desejas conversar?");
    if (novo) {
        novo = novo.trim().toLowerCase();
        const meuUser = document.getElementById('my-username').textContent;

        if (novo && novo !== meuUser) {
            if (!listaContatosLocal.has(novo)) {
                // Adiciona como offline até o servidor avisar o contrário
                listaContatosLocal.set(novo, 'OFFLINE');
            }
            atualizarInterfaceContatos();
            abrirConversa(novo);
        }
    }
});

function abrirConversa(nome) {
    contatoAtual = nome;
    currentChatTitle.textContent = nome;

    const status = listaContatosLocal.get(nome) || 'OFFLINE';
    document.getElementById('current-recipient-badge').textContent = status === 'ONLINE' ? 'Online' : 'Offline';

    messageInput.disabled = false;
    messageInput.placeholder = "Escreve a tua mensagem...";
    sendButton.disabled = false;

    chatMessages.innerHTML = '';
    ws.send(`CARREGAR_HISTORICO;${nome}`);

    atualizarInterfaceContatos(); // Atualiza cores da lista
}

function processarListaServidor(textoLista) {
    const contatos = textoLista.split(',');
    const meuUser = document.getElementById('my-username').textContent;

    contatos.forEach(c => {
        if (!c) return;
        const [nome, status] = c.split(':');

        if (nome !== meuUser) {
            // Atualiza ou insere o status verdadeiro que veio do servidor
            listaContatosLocal.set(nome, status);
        }
    });

    // Se a pessoa com quem estamos a falar mudou de estado (entrou/saiu), atualiza a etiqueta
    if (contatoAtual && listaContatosLocal.has(contatoAtual)) {
        if (document.getElementById('current-recipient-badge').textContent !== 'digitando...') {
            const statusAtual = listaContatosLocal.get(contatoAtual);
            document.getElementById('current-recipient-badge').textContent = statusAtual === 'ONLINE' ? 'Online' : 'Offline';
        }
    }

    atualizarInterfaceContatos();
}

function atualizarInterfaceContatos() {
    usersList.innerHTML = '';

    listaContatosLocal.forEach((status, nome) => {
        const div = document.createElement('div');
        div.className = `user-item ${contatoAtual === nome ? 'active' : ''}`;

        div.onclick = () => abrirConversa(nome);

        div.innerHTML = `
            <div class="status-dot ${status.toLowerCase()}"></div>
            <div class="user-info">
                <span class="user-name">${nome}</span>
                <span class="user-status">${status === 'ONLINE' ? 'Online' : 'Offline'}</span>
            </div>
        `;
        usersList.appendChild(div);
    });
}

// ... (Restante código mantém-se igual)

messageInput.addEventListener('input', () => {
    if (!contatoAtual) return;
    ws.send(`INICIO_DIGITACAO;${contatoAtual};`);
    clearTimeout(digitandoTimeout);
    digitandoTimeout = setTimeout(() => {
        ws.send(`FIM_DIGITACAO;${contatoAtual};`);
    }, 2000);
});

chatForm.addEventListener('submit', (e) => {
    e.preventDefault();
    if (!contatoAtual || !messageInput.value.trim()) return;

    const texto = messageInput.value.trim();
    ws.send(`MENSAGEM;${contatoAtual};${texto}`);
    adicionarMensagem('Você', texto, true);
    messageInput.value = '';

    clearTimeout(digitandoTimeout);
    ws.send(`FIM_DIGITACAO;${contatoAtual};`);
});

function adicionarMensagem(remetente, texto, isSentByMe) {
    const row = document.createElement('div');
    row.className = `message-row ${isSentByMe ? 'sent' : 'received'}`;
    const nomeExibicao = isSentByMe ? 'Você' : remetente;

    row.innerHTML = `
        <div class="avatar ${isSentByMe ? 'me' : 'other'}">
            ${nomeExibicao.charAt(0).toUpperCase()}
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