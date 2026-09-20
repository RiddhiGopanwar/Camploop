/* ==========================================================================
   Camploop — shared behaviors: navbar auth state, toast, product card render
   ========================================================================== */

const CATEGORY_EMOJI = {
  BOOKS_TEXTBOOKS: '📚',
  ELECTRONICS: '🔌',
  CALCULATORS: '🧮',
  HOSTEL_ESSENTIALS: '🧺',
  FASHION: '👕',
  BAGS_ACCESSORIES: '🎒',
  CYCLES: '🚲',
  GAMING: '🎮',
  ACADEMIC_SUPPLIES: '✏️',
  OTHERS: '📦'
};

function toast(message, isError) {
  const el = document.getElementById('toast');
  if (!el) return;
  el.textContent = message;
  el.classList.toggle('error', !!isError);
  el.classList.add('show');
  clearTimeout(el._timer);
  el._timer = setTimeout(() => el.classList.remove('show'), 2800);
}

function formatPrice(p) {
  if (p === null || p === undefined) return null;
  const n = Number(p);
  return '₹' + n.toLocaleString('en-IN');
}

function timeAgo(iso) {
  if (!iso) return '';
  const diffMs = Date.now() - new Date(iso).getTime();
  const mins = Math.floor(diffMs / 60000);
  if (mins < 60) return mins <= 1 ? 'just now' : `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h ago`;
  const days = Math.floor(hrs / 24);
  if (days < 7) return `${days}d ago`;
  return new Date(iso).toLocaleDateString();
}

function initials(name) {
  if (!name) return '?';
  return name.trim().split(/\s+/).slice(0, 2).map(w => w[0].toUpperCase()).join('');
}

/** Renders the nav's right-hand auth area (login/signup vs. avatar chip). */
async function initNavAuth() {
  const slot = document.getElementById('nav-auth-slot');
  if (!slot) return;

  try {
    const user = await Api.me();
    slot.innerHTML = `
      <a href="sell.html" class="btn btn-primary btn-sm">Sell an Item</a>
      <a href="profile.html" class="avatar-chip">
        <span class="avatar-circle">${initials(user.name)}</span>
        ${user.name.split(' ')[0]}
      </a>
    `;
  } catch (e) {
    slot.innerHTML = `
      <a href="login.html" class="btn btn-outline btn-sm">Log In</a>
      <a href="signup.html" class="btn btn-primary btn-sm">Sign Up</a>
    `;
  }
}

function navToggle() {
  const links = document.querySelector('.nav-links');
  if (links) links.classList.toggle('hidden');
}

/** Builds a product card element (used on homepage + marketplace). */
function productCardHTML(p, wishlistIds) {
  const emoji = CATEGORY_EMOJI[p.category] || '📦';
  let priceHTML;
  if (p.listingType === 'SELL' && p.sellingPrice != null) {
    priceHTML = `<span class="product-price">${formatPrice(p.sellingPrice)}</span>`;
    if (p.savingsPercent) {
      priceHTML += `<span class="save-badge">-${p.savingsPercent}%</span>`;
    }
  } else if (p.listingType === 'DONATE') {
    priceHTML = `<span class="product-price">Free</span>`;
  } else {
    priceHTML = `<span class="product-price">Exchange</span>`;
  }

  const isWished = wishlistIds && wishlistIds.has(p.id);
  const thumbColors = ['#FFE3D9', '#FFF3C9', '#DCE7D3', '#DCE4F7', '#F6DCE9'];
  const bg = thumbColors[p.id % thumbColors.length];
  const thumb = (p.images && p.images.length)
    ? `<img src="${p.images[0]}" alt="${escapeHtml(p.name)}" style="width:100%;height:100%;object-fit:cover">`
    : `<span>${emoji}</span>`;

  return `
    <a href="product.html?id=${p.id}" class="product-card">
      <div class="product-thumb" style="background:${bg}">
        <span class="product-tag ${p.listingType.toLowerCase()}">${p.listingType}</span>
        <button type="button" class="wish-btn ${isWished ? 'active' : ''}" data-wish-id="${p.id}" onclick="event.preventDefault(); toggleWishlistBtn(${p.id}, this)">${isWished ? '♥' : '♡'}</button>
        ${thumb}
      </div>
      <div class="product-body">
        <div class="product-cat">${p.categoryLabel}${p.pickupLocation ? ' · 📍 ' + escapeHtml(p.pickupLocation) : ''}</div>
        <h4 class="product-name">${escapeHtml(p.name)}</h4>
        <div class="product-seller">by ${escapeHtml(p.sellerName)}</div>
        <div class="product-meta">
          ${priceHTML}
          <span class="product-cond">${p.conditionLabel}</span>
        </div>
      </div>
    </a>
  `;
}

function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str == null ? '' : str;
  return div.innerHTML;
}

async function toggleWishlistBtn(productId, btnEl) {
  try {
    if (btnEl.classList.contains('active')) {
      await Api.removeWishlist(productId);
      btnEl.classList.remove('active');
      btnEl.textContent = '♡';
      toast('Removed from wishlist');
    } else {
      await Api.addWishlist(productId);
      btnEl.classList.add('active');
      btnEl.textContent = '♥';
      toast('Added to wishlist');
    }
  } catch (e) {
    if (e.message.includes('log in')) {
      toast('Log in to save items to your wishlist', true);
    } else {
      toast(e.message, true);
    }
  }
}

document.addEventListener('DOMContentLoaded', initNavAuth);
