<template>
  <div class="login-page">
    <!-- 编辑风故事面板 -->
    <aside class="login-page__art">
      <div class="login-page__art-inner">
        <div class="login-page__brand-row login-page__anim" style="animation-delay: 0ms">
          <span class="login-page__brand-dot" aria-hidden="true" />
          <span class="login-page__brand-name">RULE&nbsp;ENGINE</span>
          <span class="login-page__brand-tag">v1.0 · 2026</span>
        </div>

        <h1 class="login-page__hed login-page__anim" style="animation-delay: 80ms">
          规则<br />
          <em>即文案,</em><br />
          规则即发布。
        </h1>

        <p class="ledger login-page__deck login-page__anim" style="animation-delay: 160ms">
          面向规则中台的规则审核 · 风控 · 定价规则工作台——把 DRL
          还给业务人员,把合规留给字段,把版本还给时间。
        </p>

        <!-- 代码诗:活规则,被版本化、灰度、回滚 -->
        <div class="login-page__poem login-page__anim" aria-hidden="true" style="animation-delay: 240ms">
          <div class="login-page__poem-rule">
            <span class="login-page__poem-kw">rule</span>
            <span class="login-page__poem-name">"规则 · A01 · 通用规则"</span>
          </div>
          <div class="login-page__poem-line">
            <span class="login-page__poem-kw">when</span>
          </div>
          <div class="login-page__poem-line login-page__poem-line--indent">
            <span class="login-page__poem-var">$fact</span>
            <span class="login-page__poem-colon">:</span>
            <span class="login-page__poem-type">规则平台</span>
            <span class="login-page__poem-paren">(</span>
          </div>
          <div class="login-page__poem-line login-page__poem-line--deep">
            age&nbsp;&gt;&nbsp;60 <span class="login-page__poem-op">&amp;&amp;</span>
          </div>
          <div class="login-page__poem-line login-page__poem-line--deep">
            product.code&nbsp;<span class="login-page__poem-op">==</span>
            <span class="login-page__poem-str">"CRITICAL_ILLNESS"</span>
            <span class="login-page__poem-paren">)</span>
          </div>
          <div class="login-page__poem-line">
            <span class="login-page__poem-kw">then</span>
          </div>
          <div class="login-page__poem-line login-page__poem-line--indent">
            $result.setDecision(
          </div>
          <div class="login-page__poem-line login-page__poem-line--deep">
            DecisionType.<span class="login-page__poem-const">MANUAL_REVIEW</span>
          </div>
          <div class="login-page__poem-line login-page__poem-line--indent">
            , salience =&nbsp;<span class="login-page__poem-num">80</span>
            );
          </div>
          <div class="login-page__poem-rule">
            <span class="login-page__poem-kw">end</span>
          </div>
        </div>

        <ul class="login-page__values login-page__anim" style="animation-delay: 320ms">
          <li>
            <span class="login-page__values-idx">01</span>
            <span class="login-page__values-label">可视化 + DRL 双向转换</span>
          </li>
          <li>
            <span class="login-page__values-idx">02</span>
            <span class="login-page__values-label">版本 · 审批 · 灰度 · 回滚</span>
          </li>
          <li>
            <span class="login-page__values-idx">03</span>
            <span class="login-page__values-label">AES 加密敏感规则</span>
          </li>
          <li>
            <span class="login-page__values-idx">04</span>
            <span class="login-page__values-label">SHA-256 审计链</span>
          </li>
        </ul>
      </div>

      <!-- 背景数字装饰 -->
      <div class="login-page__counter" aria-hidden="true">
        ED.&nbsp;26&nbsp;·&nbsp;07
      </div>
    </aside>

    <!-- 登录表单 -->
    <main class="login-page__form">
      <article class="login-card login-page__anim" style="animation-delay: 200ms">
        <header class="login-card__head">
          <div class="login-card__logo">
            <Settings :size="22" stroke-width="2" />
          </div>
          <p class="login-card__eyebrow">通用规则平台 · RULE · ENGINE</p>
          <h2 class="login-card__title">登录工作台</h2>
          <p class="login-card__sub">以继续至通用规则编排中心</p>
        </header>

        <el-form class="login-card__fields" :model="loginForm" @submit.prevent>
          <el-form-item>
            <el-input v-model="loginForm.username" size="large" placeholder="用户名" autocomplete="username" />
          </el-form-item>
          <el-form-item>
            <el-input
              v-model="loginForm.password"
              size="large"
              type="password"
              placeholder="密码"
              autocomplete="current-password"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>
        </el-form>

        <el-button
          size="large"
          type="primary"
          class="login-card__cta"
          :loading="loginLoading"
          @click="handleLogin"
        >
          <LogIn :size="16" />
          <span>登录</span>
        </el-button>

        <footer class="login-card__foot">
          <span class="login-card__hint">身份、角色与权限由 <code>Sa-Token</code> 管理</span>
          <span class="login-card__copy">© 2026 规则引擎团队</span>
        </footer>
      </article>
    </main>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Settings, LogIn } from 'lucide-vue-next'
import { login as loginApi } from '../api/auth'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()
const router = useRouter()

const loginLoading = ref(false)
const loginForm = reactive({
  username: 'admin',
  password: ''
})

async function handleLogin() {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loginLoading.value = true
  try {
    authStore.completeLogin(await loginApi(loginForm))
    ElMessage.success(`欢迎回来, ${authStore.displayName}`)
    router.replace('/dashboard')
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || err?.message || '登录失败')
  } finally {
    loginLoading.value = false
  }
}
</script>

<style scoped>
/* ===========================================================
   LOGIN  —  编辑杂志感  +  代码诗
   · 左栏: 深靛蓝 + 细长 serif 大标题 + 等宽代码诗 + 编号价值点
   · 右栏: 浅米色 + 居中卡片 + 大 CTA 按钮
   =========================================================== */

.login-page {
  display: grid;
  grid-template-columns: minmax(520px, 1.4fr) minmax(420px, 1fr);
  height: 100vh;
  width: 100vw;
  overflow: hidden;
}

/* ---------- 左栏: 故事面板 ---------- */
.login-page__art {
  position: relative;
  padding: var(--sp-10) var(--sp-10) var(--sp-12);
  background: linear-gradient(160deg, #0b1020 0%, #1e1b4b 40%, #312e81 100%);
  color: #e0e7ff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  isolation: isolate;
}

/* 微妙的对角线条纹背景 */
.login-page__art::before {
  content: "";
  position: absolute;
  inset: 0;
  background-image: repeating-linear-gradient(
    135deg,
    transparent 0,
    transparent 24px,
    rgba(255,255,255,0.018) 24px,
    rgba(255,255,255,0.018) 25px
  );
  pointer-events: none;
}
.login-page__art::after {
  content: "";
  position: absolute;
  width: 520px;
  height: 520px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(99,102,241,0.18), transparent 70%);
  top: -180px;
  right: -160px;
  pointer-events: none;
}

.login-page__art-inner {
  position: relative;
  max-width: 640px;
  display: flex;
  flex-direction: column;
  gap: var(--sp-6);
  flex: 1;
}

.login-page__brand-row {
  display: flex;
  align-items: center;
  gap: var(--sp-3);
  z-index: 1;
}
.login-page__brand-dot {
  width: 10px;
  height: 10px;
  border-radius: 2px;
  background: var(--color-primary-400);
  box-shadow: 0 0 0 4px rgba(99,102,241,0.2);
}
.login-page__brand-name {
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.3em;
  color: #fff;
}
.login-page__brand-tag {
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--color-primary-300);
  padding: 2px 8px;
  border: 1px solid rgba(99,102,241,0.4);
  border-radius: var(--radius-full);
  letter-spacing: 0.08em;
}

.login-page__hed {
  margin: 0;
  font-family: var(--font-serif);
  font-weight: 700;
  font-size: clamp(44px, 5vw, 72px);
  line-height: var(--lh-display);
  letter-spacing: var(--tracking-tight);
  color: #fff;
  z-index: 1;
}
.login-page__hed em {
  font-style: italic;
  font-weight: 500;
  color: var(--color-primary-300);
}

.login-page__deck {
  margin: 0;
  font-size: var(--fs-md);
  line-height: var(--lh-body);
  color: rgba(224,231,255,0.7);
  max-width: 460px;
  z-index: 1;
}

/* 代码诗 */
.login-page__poem {
  position: relative;
  padding: var(--sp-5);
  background: rgba(0,0,0,0.35);
  border-radius: var(--radius-md);
  border: 1px solid rgba(99,102,241,0.2);
  z-index: 1;
  overflow: hidden;
  transition: background var(--dur-3) var(--ease);
}
.login-page__poem:hover {
  background: rgba(0,0,0,0.45);
}
.login-page__poem::before {
  content: "// RULE_POEM";
  position: absolute;
  top: var(--sp-3);
  right: var(--sp-3);
  font-family: var(--font-mono);
  font-size: 10px;
  color: var(--color-primary-300);
  letter-spacing: 0.1em;
  opacity: 0.6;
}
.login-page__poem-rule {
  margin: var(--sp-2) 0;
}
.login-page__poem-line {
  font-family: var(--font-mono);
  font-size: 13px;
  line-height: 1.7;
  color: rgba(224,231,255,0.85);
}
.login-page__poem-line--indent { padding-left: var(--sp-5); }
.login-page__poem-line--deep   { padding-left: var(--sp-10); color: #c7d2fe; }
.login-page__poem-kw    { color: #f472b6; font-weight: 600; }
.login-page__poem-name  { color: #fde68a; margin-left: 6px; }
.login-page__poem-colon { color: rgba(255,255,255,0.4); }
.login-page__poem-type  { color: #67e8f9; }
.login-page__poem-paren { color: rgba(255,255,255,0.5); }
.login-page__poem-var   { color: #a5f3fc; }
.login-page__poem-op    { color: rgba(255,255,255,0.5); }
.login-page__poem-str   { color: #86efac; }
.login-page__poem-const { color: #fde68a; font-weight: 600; }
.login-page__poem-num   { color: #fbbf24; }

/* 编号价值点 */
.login-page__values {
  list-style: none;
  padding: 0;
  margin: auto 0 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--sp-3);
  z-index: 1;
}
.login-page__values li {
  display: flex;
  align-items: baseline;
  gap: var(--sp-3);
  padding: var(--sp-3);
  border: 1px solid rgba(99,102,241,0.15);
  border-radius: var(--radius-md);
  background: rgba(0,0,0,0.15);
  transition: border-color var(--dur-2) var(--ease), transform var(--dur-2) var(--ease);
}
.login-page__values li:hover {
  border-color: rgba(99,102,241,0.5);
  transform: translateY(-1px);
}
.login-page__values-idx {
  font-family: var(--font-serif);
  font-size: 20px;
  font-style: italic;
  color: var(--color-primary-300);
  min-width: 32px;
}
.login-page__values-label {
  font-size: var(--fs-sm);
  color: rgba(224,231,255,0.8);
  line-height: var(--lh-tight);
}

/* 刊脚期号 */
.login-page__counter {
  position: absolute;
  bottom: var(--sp-6);
  right: var(--sp-6);
  font-family: var(--font-serif);
  font-size: 14px;
  letter-spacing: 0.2em;
  color: rgba(255,255,255,0.3);
  z-index: 1;
}

/* ---------- 右栏: 表单 ---------- */
.login-page__form {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--sp-10);
  background: linear-gradient(180deg, #fafafa 0%, #f1f5f9 100%);
}
.login-card {
  width: 100%;
  max-width: 400px;
}

.login-card__head {
  margin-bottom: var(--sp-10);
}
.login-card__logo {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, #4f46e5, #6366f1);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--sp-4);
  box-shadow: 0 12px 32px rgba(79,70,229,0.25);
}
.login-card__eyebrow {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.3em;
  color: var(--color-neutral-500);
  margin: 0 0 var(--sp-2);
  text-transform: uppercase;
}
.login-card__title {
  font-family: var(--font-serif);
  font-size: var(--fs-2xl);
  font-weight: 700;
  color: var(--color-neutral-900);
  margin: 0;
  line-height: var(--lh-tight);
}
.login-card__sub {
  margin: var(--sp-2) 0 0;
  color: var(--color-neutral-500);
  font-size: var(--fs-sm);
}

.login-card__label {
  font-family: var(--font-serif);
  font-size: var(--fs-sm);
  font-weight: 500;
  color: var(--color-neutral-700);
}

.login-card__cta {
  width: 100%;
  height: 46px;
  font-weight: 600;
  letter-spacing: 0.04em;
  gap: var(--sp-2);
  box-shadow: 0 4px 16px rgba(79,70,229,0.2);
  transition: all var(--dur-2) var(--ease);
}
.login-card__cta:hover {
  box-shadow: 0 6px 24px rgba(79,70,229,0.35);
  transform: translateY(-1px);
}
.login-card__cta:active {
  transform: translateY(0) scale(0.98);
}

.login-card__fields {
  margin-bottom: var(--sp-4);
}

.login-card__divider {
  display: flex;
  align-items: center;
  gap: var(--sp-3);
  margin: 0 0 var(--sp-4);
  color: var(--color-neutral-400);
  font-size: var(--fs-xs);
}
.login-card__divider::before,
.login-card__divider::after {
  content: "";
  flex: 1;
  height: 1px;
  background: var(--color-neutral-200);
}

.login-card__foot {
  margin-top: var(--sp-6);
  padding-top: var(--sp-4);
  border-top: 1px solid var(--color-neutral-200);
  display: flex;
  align-items: center;
  gap: var(--sp-2);
  justify-content: center;
  font-size: var(--fs-xs);
  color: var(--color-neutral-500);
}
.login-card__hint code {
  font-family: var(--font-mono);
  background: var(--color-neutral-100);
  padding: 1px 6px;
  border-radius: var(--radius-sm);
  color: var(--color-neutral-800);
  font-size: 11px;
}
.login-card__dot {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: var(--color-neutral-400);
}
.login-card__copy {
  color: var(--color-neutral-400);
}

/* ---------- Animations ---------- */
.login-page__anim {
  animation: loginFadeIn 600ms cubic-bezier(0.4, 0, 0.2, 1) both;
}
@keyframes loginFadeIn {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ---------- Responsive ---------- */
@media (max-width: 960px) {
  .login-page {
    grid-template-columns: 1fr;
  }
  .login-page__art {
    display: none;
  }
}
</style>
