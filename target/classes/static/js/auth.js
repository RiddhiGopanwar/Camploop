/* ==========================================================================
   Camploop — Supabase Auth helpers + Storage upload + "recently viewed"
   ========================================================================== */

const Auth = {
  async signUp(name, email, password) {
    const { data, error } = await supabaseClient.auth.signUp({
      email,
      password,
      options: { data: { name } } // stored as user_metadata; ProfileService also uses email as a fallback name
    });
    if (error) throw new Error(error.message);
    return data;
  },

  async signIn(email, password) {
    const { data, error } = await supabaseClient.auth.signInWithPassword({ email, password });
    if (error) throw new Error(error.message);
    return data;
  },

  async signOut() {
    await supabaseClient.auth.signOut();
  },

  async getSession() {
    const { data } = await supabaseClient.auth.getSession();
    return data.session;
  }
};

/** Uploads a File to the Supabase Storage "product-images" bucket and returns its public URL. */
async function uploadProductImage(file, userId) {
  const ext = file.name.split('.').pop();
  const path = `${userId}/${Date.now()}-${Math.random().toString(36).slice(2, 8)}.${ext}`;

  const { error } = await supabaseClient.storage.from('product-images').upload(path, file, {
    cacheControl: '3600',
    upsert: false
  });
  if (error) throw new Error(error.message);

  const { data } = supabaseClient.storage.from('product-images').getPublicUrl(path);
  return data.publicUrl;
}

/** Client-side "recently viewed" list (localStorage) — no backend table needed for this. */
const RecentlyViewed = {
  KEY: 'camploop_recently_viewed',
  MAX: 12,

  add(productId) {
    let ids = this.getIds().filter(id => id !== productId);
    ids.unshift(productId);
    ids = ids.slice(0, this.MAX);
    localStorage.setItem(this.KEY, JSON.stringify(ids));
  },

  getIds() {
    try {
      return JSON.parse(localStorage.getItem(this.KEY)) || [];
    } catch (e) {
      return [];
    }
  }
};
