/* ==========================================================================
   Camploop — private buyer <-> seller chat (small modal).
   Needs: api.js, main.js (toast, escapeHtml, timeAgo). Used by product.html
   and profile.html. The modal is created on first use, so pages don't need
   extra markup.
   ========================================================================== */

const Chat = (() => {
  let modal, logEl, inputEl, titleEl, subEl, noteEl;
  let conversation = null, myId = null, lastId = 0, timer = null;

  function build() {
    if (modal) return;
    modal = document.createElement('div');
    modal.className = 'chat-overlay hidden';
    modal.innerHTML = `
      <div class="auth-card chat-box">
        <div class="chat-head">
          <div style="min-width:0">
            <div class="chat-title" id="chat-title"></div>
            <div class="chat-sub" id="chat-sub"></div>
          </div>
          <button type="button" class="icon-btn" id="chat-close" title="Close">✖️</button>
        </div>
        <div class="chat-log" id="chat-log"></div>
        <form class="chat-form" id="chat-form">
          <input type="text" id="chat-input" maxlength="1000" autocomplete="off" placeholder="Type a message…">
          <button type="submit" class="btn btn-primary btn-sm">Send</button>
        </form>
        <div class="chat-note" id="chat-note"></div>
      </div>`;
    document.body.appendChild(modal);
    logEl = modal.querySelector('#chat-log');
    inputEl = modal.querySelector('#chat-input');
    titleEl = modal.querySelector('#chat-title');
    subEl = modal.querySelector('#chat-sub');
    noteEl = modal.querySelector('#chat-note');
    modal.querySelector('#chat-close').addEventListener('click', close);
    modal.addEventListener('click', (e) => { if (e.target === modal) close(); });
    modal.querySelector('#chat-form').addEventListener('submit', onSend);
  }

  function bubble(m) {
    const mine = m.senderId === myId;
    return `<div class="chat-msg ${mine ? 'mine' : 'theirs'}">
      <div class="chat-bubble">${escapeHtml(m.body)}</div>
      <div class="chat-time">${timeAgo(m.createdAt)}</div></div>`;
  }

  function appendMessages(list) {
    if (!list.length) return;
    const empty = logEl.querySelector('.chat-empty');
    if (empty) empty.remove();
    const nearBottom = logEl.scrollHeight - logEl.scrollTop - logEl.clientHeight < 80;
    logEl.insertAdjacentHTML('beforeend', list.map(bubble).join(''));
    lastId = list[list.length - 1].id;
    if (nearBottom) logEl.scrollTop = logEl.scrollHeight;
  }

  async function poll() {
    if (!conversation) return;
    try {
      const fresh = await Api.getMessages(conversation.id, lastId);
      appendMessages(fresh);
      if (typeof refreshNavBadge === 'function' && fresh.length) refreshNavBadge();
    } catch (e) { /* transient — try again next tick */ }
  }

  async function onSend(e) {
    e.preventDefault();
    const text = inputEl.value.trim();
    if (!text || !conversation) return;
    inputEl.value = '';
    try {
      const m = await Api.sendMessage(conversation.id, text);
      appendMessages([m]);
      logEl.scrollTop = logEl.scrollHeight;
    } catch (err) {
      inputEl.value = text;
      toast(err.message, true);
    }
  }

  /** Opens the chat UI for a conversation object returned by the API. */
  async function open(conv) {
    build();
    const me = await Api.me();
    myId = me.id;
    conversation = conv;
    lastId = 0;
    titleEl.textContent = conv.otherUserName;
    subEl.textContent = 'About: ' + conv.productName;
    noteEl.textContent = `🔒 Private — only you and ${conv.otherUserName} can see this chat. Meet on campus, and never share passwords or OTPs.`;
    logEl.innerHTML = '<div class="chat-empty">Say hi and agree on a time and place to meet 👋</div>';
    modal.classList.remove('hidden');
    await poll();
    logEl.scrollTop = logEl.scrollHeight;
    clearInterval(timer);
    timer = setInterval(poll, 4000);
    inputEl.focus();
  }

  /** Buyer: "Contact Seller". Seller: pass buyerId to chat with an interested buyer. */
  async function openForProduct(productId, buyerId) {
    try {
      const conv = await Api.startConversation(productId, buyerId);
      await open(conv);
    } catch (e) {
      toast(e.message.includes('log in') ? 'Log in to contact the seller' : e.message, true);
    }
  }

  function close() {
    clearInterval(timer);
    timer = null;
    conversation = null;
    if (modal) modal.classList.add('hidden');
  }

  return { open, openForProduct, close };
})();
