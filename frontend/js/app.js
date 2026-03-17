// API 基础路径（通过 Nginx 代理，动静分离）
const API = '';

let currentToken = localStorage.getItem('token') || '';

// ── 工具函数 ────────────────────────────────────────────

function showMsg(id, text, type = 'error') {
  const el = document.getElementById(id);
  el.textContent = text;
  el.className = `msg ${type}`;
}

function switchTab(tab) {
  document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
  event.target.classList.add('active');
  document.getElementById('loginForm').style.display    = tab === 'login'    ? 'block' : 'none';
  document.getElementById('registerForm').style.display = tab === 'register' ? 'block' : 'none';
  document.getElementById('authMsg').textContent = '';
}

function closeModal() {
  document.getElementById('modal').style.display = 'none';
}

// ── 用户注册 ─────────────────────────────────────────────

async function register() {
  const username = document.getElementById('regUsername').value.trim();
  const password = document.getElementById('regPassword').value.trim();
  const email    = document.getElementById('regEmail').value.trim();

  if (!username || !password) {
    showMsg('authMsg', '用户名和密码不能为空');
    return;
  }

  try {
    const res = await fetch(`${API}/api/user/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password, email })
    });
    const data = await res.json();
    if (data.code === 200) {
      showMsg('authMsg', `注册成功！用户ID: ${data.data.userId}`, 'success');
    } else {
      showMsg('authMsg', data.message || '注册失败');
    }
  } catch (e) {
    showMsg('authMsg', '网络错误，请检查后端服务');
  }
}

// ── 用户登录 ─────────────────────────────────────────────

async function login() {
  const username = document.getElementById('loginUsername').value.trim();
  const password = document.getElementById('loginPassword').value.trim();

  try {
    const res = await fetch(`${API}/api/user/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });
    const data = await res.json();
    if (data.code === 200) {
      currentToken = data.data.token;
      localStorage.setItem('token', currentToken);
      showMsg('authMsg', `登录成功！欢迎 ${data.data.username}`, 'success');
      document.getElementById('loginBtn').textContent = username;
    } else {
      showMsg('authMsg', data.message || '登录失败');
    }
  } catch (e) {
    showMsg('authMsg', '网络错误，请检查后端服务');
  }
}

// ── 商品详情（走 Redis 缓存接口）────────────────────────

async function getProduct(id) {
  try {
    const res = await fetch(`${API}/api/product/${id}`);
    const data = await res.json();
    if (data.code === 200) {
      const p = data.data;
      document.getElementById('modalContent').innerHTML = `
        <h2>${p.name}</h2>
        <p style="color:#ff3b30;font-size:1.4rem;margin:12px 0">¥ ${p.price}</p>
        <p style="color:#86868b;margin-bottom:16px">${p.description || '暂无描述'}</p>
        <p>库存：<strong>${p.stock ?? '加载中'}</strong></p>
        <p style="font-size:0.8rem;color:#86868b;margin-top:8px">
          数据来源：${p.fromCache ? '⚡ Redis缓存' : '🗄 MySQL数据库'}
        </p>
        <button class="btn-seckill" style="margin-top:20px;width:100%">立即抢购</button>
      `;
      document.getElementById('modal').style.display = 'flex';
    }
  } catch (e) {
    alert('获取商品详情失败：' + e.message);
  }
}

// ── 页面初始化：加载库存 ──────────────────────────────────

async function loadStocks() {
  for (let i = 1; i <= 3; i++) {
    try {
      const res = await fetch(`${API}/api/product/${i}`);
      const data = await res.json();
      const el = document.getElementById(`stock-${i}`);
      if (el && data.code === 200) {
        el.textContent = data.data.stock ?? '—';
      }
    } catch {
      const el = document.getElementById(`stock-${i}`);
      if (el) el.textContent = '—';
    }
  }
}

window.addEventListener('DOMContentLoaded', loadStocks);
