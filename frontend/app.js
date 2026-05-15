const API_BASE = localStorage.getItem("libraryApiBase") || "http://localhost:8080";
const state = {
  token: localStorage.getItem("libraryToken") || "",
  user: JSON.parse(localStorage.getItem("libraryUser") || "null"),
  books: [],
  borrows: [],
  editingBookId: null
};

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => [...document.querySelectorAll(selector)];

function toast(message) {
  const node = $("#toast");
  node.textContent = message;
  node.classList.add("is-visible");
  setTimeout(() => node.classList.remove("is-visible"), 2200);
}

async function api(path, options = {}) {
  const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
  if (state.token) headers.Authorization = `Bearer ${state.token}`;
  const response = await fetch(`${API_BASE}${path}`, { ...options, headers });
  if (!response.ok) {
    let detail = `${response.status}`;
    try {
      const body = await response.json();
      detail = body.detail || body.title || detail;
    } catch {}
    throw new Error(detail);
  }
  if (response.status === 204) return null;
  return response.json();
}

function showAuthTab(tab) {
  $$("[data-auth-tab]").forEach((btn) => btn.classList.toggle("is-active", btn.dataset.authTab === tab));
  $("#loginForm").classList.toggle("is-hidden", tab !== "login");
  $("#registerForm").classList.toggle("is-hidden", tab !== "register");
}

function syncRoleUi() {
  const admin = state.user?.role === "ADMIN";
  $$(".admin-only").forEach((node) => node.classList.toggle("is-hidden", !admin));
}

function enterWorkspace() {
  $("#authShell").classList.add("is-hidden");
  $("#workspace").classList.remove("is-hidden");
  $("#currentUser").textContent = `${state.user.username} · ${state.user.role}`;
  syncRoleUi();
  loadAll();
}

function logout() {
  state.token = "";
  state.user = null;
  localStorage.removeItem("libraryToken");
  localStorage.removeItem("libraryUser");
  $("#workspace").classList.add("is-hidden");
  $("#authShell").classList.remove("is-hidden");
  showAuthTab("login");
}

function setView(view) {
  const copy = {
    dashboard: ["概览", "查看系统状态和当前借阅情况"],
    books: ["图书", "浏览、搜索并发起借阅"],
    borrows: ["我的借阅", "查看借阅记录并归还图书"],
    manage: ["图书管理", "管理员维护书目和库存"]
  };
  $$(".nav-item").forEach((btn) => btn.classList.toggle("is-active", btn.dataset.view === view));
  $$(".view").forEach((node) => node.classList.add("is-hidden"));
  $(`#${view}View`).classList.remove("is-hidden");
  $("#viewTitle").textContent = copy[view][0];
  $("#viewSubtitle").textContent = copy[view][1];
}

async function loadAll() {
  await Promise.all([loadBooks(), loadBorrows(), loadInstance()]);
  renderDashboard();
}

async function loadBooks(query = "", category = "") {
  const params = new URLSearchParams();
  if (query) params.set("q", query);
  if (category) params.set("category", category);
  const path = params.size ? `/api/books/search?${params}` : "/api/books";
  state.books = await api(path);
  renderBooks();
  renderManageTable();
}

async function loadBorrows() {
  state.borrows = state.token ? await api("/api/borrows") : [];
  renderBorrows();
}

async function loadInstance() {
  const instance = await api("/api/books/instance");
  $("#instanceChip").textContent = `${instance.service} · ${instance.port}`;
}

function renderDashboard() {
  $("#metricBooks").textContent = state.books.length;
  $("#metricStock").textContent = state.books.reduce((sum, book) => sum + book.stock, 0);
  $("#metricBorrows").textContent = state.borrows.length;
  $("#recentBooks").innerHTML = state.books.slice(0, 4).map((book) => `
    <div class="compact-item"><span>${book.title}</span><strong>${book.stock}</strong></div>
  `).join("") || `<span class="muted">暂无图书</span>`;
  $("#recentBorrows").innerHTML = state.borrows.slice(0, 4).map((item) => `
    <div class="compact-item"><span>图书 #${item.bookId}</span><strong>${item.status}</strong></div>
  `).join("") || `<span class="muted">暂无借阅记录</span>`;
}

function renderBooks() {
  $("#booksGrid").innerHTML = state.books.map((book) => `
    <article class="book-card">
      <div>
        <h3>${book.title}</h3>
        <div class="book-meta">
          <span>${book.author}</span>
          <span>${book.category || "未分类"} · ${book.publisher || "未知出版社"}</span>
          <span>ISBN ${book.isbn || "--"}</span>
        </div>
      </div>
      <div class="book-actions">
        <span>库存 <strong>${book.stock}</strong></span>
        <button data-borrow-id="${book.id}" ${book.stock <= 0 ? "disabled" : ""}>借阅</button>
      </div>
    </article>
  `).join("") || `<div class="panel">没有匹配的图书</div>`;
}

function renderBorrows() {
  $("#borrowTable").innerHTML = `
    <div class="table-row table-head"><span>记录</span><span>图书</span><span>借出时间</span><span>状态</span><span>操作</span></div>
    ${state.borrows.map((item) => `
      <div class="table-row">
        <span>#${item.id}</span>
        <span>图书 #${item.bookId}</span>
        <span>${formatDate(item.borrowTime)}</span>
        <span>${item.status}</span>
        <span>${item.status === "BORROWED" || item.status === "OVERDUE"
          ? `<button data-return-id="${item.id}">归还</button>`
          : `<span class="muted">已完成</span>`}</span>
      </div>
    `).join("")}
  `;
}

function renderManageTable() {
  $("#manageTable").innerHTML = `
    <div class="table-row table-head"><span>书名</span><span>作者</span><span>库存</span><span>价格</span><span>操作</span></div>
    ${state.books.map((book) => `
      <div class="table-row">
        <span>${book.title}</span>
        <span>${book.author}</span>
        <span>${book.stock}</span>
        <span>${book.price}</span>
        <span>
          <button data-edit-id="${book.id}">编辑</button>
          <button data-delete-id="${book.id}">删除</button>
        </span>
      </div>
    `).join("")}
  `;
}

function formatDate(value) {
  return value ? new Date(value).toLocaleString("zh-CN") : "--";
}

async function borrowBook(id) {
  try {
    await api("/api/borrows", { method: "POST", body: JSON.stringify({ bookId: id }) });
    toast("借阅成功");
    await loadAll();
  } catch (error) {
    toast(error.message);
  }
}

async function returnBook(id) {
  try {
    await api(`/api/borrows/${id}/return`, { method: "PUT" });
    toast("归还成功");
    await loadAll();
  } catch (error) {
    toast(error.message);
  }
}

function fillBookForm(book) {
  const form = $("#bookForm");
  Object.entries(book).forEach(([key, value]) => {
    if (form.elements[key]) form.elements[key].value = value ?? "";
  });
  state.editingBookId = book.id;
  $("#bookFormTitle").textContent = "编辑图书";
}

function clearBookForm() {
  $("#bookForm").reset();
  state.editingBookId = null;
  $("#bookFormTitle").textContent = "新增图书";
}

async function saveBook(form) {
  const body = Object.fromEntries(new FormData(form));
  body.stock = Number(body.stock);
  body.price = Number(body.price);
  try {
    if (state.editingBookId) {
      await api(`/api/books/${state.editingBookId}`, { method: "PUT", body: JSON.stringify(body) });
      toast("图书已更新");
    } else {
      await api("/api/books", { method: "POST", body: JSON.stringify(body) });
      toast("图书已创建");
    }
    clearBookForm();
    await loadBooks();
    renderDashboard();
  } catch (error) {
    toast(error.message);
  }
}

async function deleteBook(id) {
  try {
    await api(`/api/books/${id}`, { method: "DELETE" });
    toast("图书已删除");
    await loadBooks();
    renderDashboard();
  } catch (error) {
    toast(error.message);
  }
}

$$("[data-auth-tab]").forEach((btn) => btn.addEventListener("click", () => showAuthTab(btn.dataset.authTab)));
$("#loginForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const body = Object.fromEntries(new FormData(event.currentTarget));
  try {
    const result = await api("/api/auth/login", { method: "POST", body: JSON.stringify(body) });
    state.token = result.token;
    state.user = result;
    localStorage.setItem("libraryToken", state.token);
    localStorage.setItem("libraryUser", JSON.stringify(result));
    enterWorkspace();
  } catch (error) {
    toast(error.message);
  }
});
$("#registerForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  const body = Object.fromEntries(new FormData(form));
  if (form.elements.admin.checked) body.role = "ADMIN";
  delete body.admin;
  try {
    await api("/api/users/register", { method: "POST", body: JSON.stringify(body) });
    toast("注册成功，请登录");
    form.reset();
    showAuthTab("login");
  } catch (error) {
    toast(error.message);
  }
});
$("#logoutBtn").addEventListener("click", logout);
$$(".nav-item").forEach((btn) => btn.addEventListener("click", () => setView(btn.dataset.view)));
$("#searchBooksBtn").addEventListener("click", () => loadBooks($("#bookQuery").value.trim(), $("#bookCategory").value.trim()));
$("#resetBooksBtn").addEventListener("click", () => {
  $("#bookQuery").value = "";
  $("#bookCategory").value = "";
  loadBooks();
});
$("#booksGrid").addEventListener("click", (event) => {
  const id = event.target.dataset.borrowId;
  if (id) borrowBook(Number(id));
});
$("#borrowTable").addEventListener("click", (event) => {
  const id = event.target.dataset.returnId;
  if (id) returnBook(Number(id));
});
$("#bookForm").addEventListener("submit", (event) => {
  event.preventDefault();
  saveBook(event.currentTarget);
});
$("#cancelEditBtn").addEventListener("click", clearBookForm);
$("#manageTable").addEventListener("click", (event) => {
  const editId = event.target.dataset.editId;
  const deleteId = event.target.dataset.deleteId;
  if (editId) {
    const book = state.books.find((item) => item.id === Number(editId));
    if (book) fillBookForm(book);
  }
  if (deleteId) deleteBook(Number(deleteId));
});

if (state.token && state.user) {
  enterWorkspace();
}
