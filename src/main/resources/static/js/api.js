/* ==========================================================================
   Camploop — tiny fetch wrapper for the Spring Boot backend (/api).
   Auth itself is handled by Supabase directly (see auth.js); every call
   here attaches the current Supabase session's access token so the
   backend's SupabaseJwtFilter can identify the logged-in student.
   ========================================================================== */

const API_BASE = '/api';

async function authHeader() {
  const { data } = await supabaseClient.auth.getSession();
  const token = data.session ? data.session.access_token : null;
  return token ? { 'Authorization': 'Bearer ' + token } : {};
}

async function apiRequest(path, options = {}) {
  const headers = await authHeader();

  const res = await fetch(API_BASE + path, {
    method: options.method || 'GET',
    headers: {
      'Content-Type': 'application/json',
      ...headers,
      ...(options.headers || {})
    },
    body: options.body ? JSON.stringify(options.body) : undefined
  });

  let data = null;
  try {
    data = await res.json();
  } catch (e) {
    data = null;
  }

  if (!res.ok) {
    const message = (data && data.message) ? data.message : `Request failed (${res.status})`;
    throw new Error(message);
  }

  return data;
}

const Api = {
  // Profile (auth itself is via supabaseClient.auth.* — see auth.js)
  me: () => apiRequest('/profiles/me'),
  updateMe: (payload) => apiRequest('/profiles/me', { method: 'PUT', body: payload }),
  getProfile: (id) => apiRequest(`/profiles/${id}`), // public seller view — no auth required

  recentProducts: () => apiRequest('/products/recent'),
  searchProducts: (params) => apiRequest('/products?' + new URLSearchParams(params).toString()),
  getProduct: (id) => apiRequest(`/products/${id}`),
  myProducts: () => apiRequest('/products/mine'),
  createProduct: (payload) => apiRequest('/products', { method: 'POST', body: payload }),
  updateProduct: (id, payload) => apiRequest(`/products/${id}`, { method: 'PUT', body: payload }),
  deleteProduct: (id) => apiRequest(`/products/${id}`, { method: 'DELETE' }),
  markProductStatus: (id, status) => apiRequest(`/products/${id}/status?status=${status}`, { method: 'PATCH' }),

  myWishlist: () => apiRequest('/wishlist'),
  addWishlist: (productId) => apiRequest(`/wishlist/${productId}`, { method: 'POST' }),
  removeWishlist: (productId) => apiRequest(`/wishlist/${productId}`, { method: 'DELETE' }),

  reportProduct: (productId, reason) => apiRequest(`/reports/${productId}`, { method: 'POST', body: { reason } }),
};
