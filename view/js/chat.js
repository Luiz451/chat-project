const urlParams = new URLSearchParams(window.location.search);
const porta = urlParams.get("port") || "12345";
const ws = new WebSocket(`ws://localhost:${porta}`);

const messageInput = document.getElementById("message-input");
const sendButton = document.getElementById("send-button");
const chatForm = document.getElementById("chat-form");
const chatMessages = document.getElementById("chat-messages");
const currentChatTitle = document.getElementById("current-chat-title");
const connectionBadge = document.getElementById("connection-badge");
const connectionStatusDot = document.getElementById("connection-status-dot");

let meuUser = null;
let contatoAtual = null;

ws.onopen = () => {
  connectionBadge.textContent = "Conectado";
  connectionBadge.style.backgroundColor = "#10b981";
  connectionStatusDot.classList.replace("offline", "online");

  meuUser = prompt("Qual é o seu nome de usuário?") || "Anônimo";
  document.getElementById("my-username").textContent = meuUser;

  ws.send(`PEDIDO_ACESSO;${meuUser};123`);

  // Pequeno delay para garantir o login antes de carregar ou listar
  setTimeout(() => {
    carregarListaUsuarios();
  }, 500);
};

ws.onmessage = (event) => {
  const mensagemRecebida = event.data;

  // Tratamento da lista de usuários vinda do Java (usando | e limpando colchetes)
  if (mensagemRecebida.startsWith("RESPOSTA_LISTA_USUARIOS|")) {
    const partes = mensagemRecebida.split("|", 2);
    const listaLimpa = partes[1]
      ? partes[1].replace("[", "").replace("]", "")
      : "";
    const usuarios = listaLimpa
      ? listaLimpa.split(",").map((u) => u.trim())
      : [];
    exibirListaUsuariosNaTela(usuarios);
    return;
  }

  // Tratamento padrão de mensagens de chat
  const dados = mensagemRecebida.split("|", 2);
  if (dados.length === 2) {
    const remetente = dados[0];
    const texto = dados[1];

    const isSentByMe = remetente === meuUser;
    adicionarMensagem(remetente, texto, isSentByMe);
  }
};

ws.onclose = () => {
  connectionBadge.textContent = "Desconectado";
  connectionBadge.style.backgroundColor = "#ef4444";
  connectionStatusDot.classList.replace("online", "offline");
};

function carregarListaUsuarios() {
  if (ws.readyState === WebSocket.OPEN) {
    ws.send(`LISTAR_USUARIOS;${meuUser};`);
  }
}

function exibirListaUsuariosNaTela(usuarios) {
  console.log("Usuários disponíveis:", usuarios);

  // Corrigido para "users-list" conforme o seu ID no HTML
  const userListContainer = document.getElementById("users-list");
  if (!userListContainer) return;

  userListContainer.innerHTML = "";
  usuarios.forEach((usuario) => {
    if (usuario === meuUser) return; // Não lista a si mesmo

    const item = document.createElement("div");
    item.className = "user-item";
    item.textContent = usuario;
    item.onclick = () => {
      contatoAtual = usuario;
      currentChatTitle.textContent = contatoAtual;
      messageInput.disabled = false;
      sendButton.disabled = false;

      chatMessages.innerHTML = "";
      ws.send(`CARREGAR_HISTORICO;${contatoAtual}`);
    };
    userListContainer.appendChild(item);
  });
}

chatForm.addEventListener("submit", (e) => {
  e.preventDefault();
  if (!contatoAtual || !messageInput.value.trim()) return;

  const texto = messageInput.value.trim();

  ws.send(`MENSAGEM;${contatoAtual};${texto}`);

  adicionarMensagem("Você", texto, true);
  messageInput.value = "";
});

function adicionarMensagem(remetente, texto, isSentByMe) {
  const row = document.createElement("div");
  row.className = `message-row ${isSentByMe ? "sent" : "received"}`;

  const nomeExibicao = isSentByMe ? "Você" : remetente;
  const inicial = nomeExibicao.charAt(0).toUpperCase();

  row.innerHTML = `
      <div class="avatar ${isSentByMe ? "me" : "other"}">
          ${inicial}
      </div>
      <div class="message-content">
          <span class="message-info">${nomeExibicao}</span>
          <div class="bubble ${isSentByMe ? "sent-bubble" : "other-bubble"}">
              ${texto}
          </div>
      </div>
  `;

  chatMessages.appendChild(row);
  chatMessages.scrollTop = chatMessages.scrollHeight;
}
