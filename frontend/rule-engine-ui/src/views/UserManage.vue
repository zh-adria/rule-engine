<template>
  <div class="user-manage">
    <!-- ====== Masthead ====== -->
    <header class="user-manage__mast">
      <p class="user-manage__kicker">IAM · 身份管理</p>
      <h1 class="user-manage__hed">身份与权限</h1>
      <p class="user-manage__deck">
        用户、角色、权限由 Sa-Token 统一管理。规则引擎消费 Gateway 透传的身份上下文。
      </p>
    </header>

    <!-- ====== Stats Grid ====== -->
    <section class="user-manage__stats">
      <article class="user-stat">
        <div class="user-stat__icon">
          <Users :size="18" />
        </div>
        <div class="user-stat__body">
          <span class="user-stat__num">—</span>
          <span class="user-stat__label">用户</span>
        </div>
      </article>
      <article class="user-stat">
        <div class="user-stat__icon user-stat__icon--role">
          <Shield :size="18" />
        </div>
        <div class="user-stat__body">
          <span class="user-stat__num">—</span>
          <span class="user-stat__label">角色</span>
        </div>
      </article>
      <article class="user-stat">
        <div class="user-stat__icon user-stat__icon--perm">
          <KeyRound :size="18" />
        </div>
        <div class="user-stat__body">
          <span class="user-stat__num">—</span>
          <span class="user-stat__label">权限</span>
        </div>
      </article>
      <article class="user-stat">
        <div class="user-stat__icon user-stat__icon--org">
          <Building2 :size="18" />
        </div>
        <div class="user-stat__body">
          <span class="user-stat__num">—</span>
          <span class="user-stat__label">组织</span>
        </div>
      </article>
    </section>

    <!-- ====== Info Panel ====== -->
    <section class="user-manage__panel">
      <div class="user-manage__panel-inner">
        <div class="user-manage__brand">
          <div class="user-manage__brand-icon">
            <ShieldCheck :size="28" />
          </div>
          <div>
            <h2 class="user-manage__panel-title">Sa-Token 统一认证</h2>
            <p class="user-manage__panel-desc">
              身份、角色、权限和租户由 Sa-Token 管理。规则引擎只消费 Gateway 透传的身份上下文。
            </p>
          </div>
        </div>
      </div>

      <!-- 底部信息条 -->
      <div class="user-manage__panel-foot">
        <div class="user-manage__foot-item">
          <span class="user-manage__foot-label">认证方式</span>
          <span class="user-manage__foot-value">Sa-Token</span>
        </div>
        <div class="user-manage__foot-sep" />
        <div class="user-manage__foot-item">
          <span class="user-manage__foot-label">网关</span>
          <span class="user-manage__foot-value">Gateway 透传</span>
        </div>
        <div class="user-manage__foot-sep" />
        <div class="user-manage__foot-item">
          <span class="user-manage__foot-label">状态</span>
          <span class="re-tag re-tag--success">
            <span class="re-tag__dot" /> 已接入
          </span>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ShieldCheck, Users, Shield, KeyRound, Building2 } from 'lucide-vue-next'
</script>

<style scoped>
/* ===========================================================
   USER MANAGE — taste-skill anti-slop redesign
   · Editorial masthead with brand mark
   · Stats grid (4 KPI-style cards)
   · Center panel with action buttons
   · Bottom info bar with status
   =========================================================== */

.user-manage {
  padding-bottom: var(--sp-8);
}

/* ---------- Masthead ---------- */
.user-manage__mast {
  margin-bottom: var(--sp-6);
  padding-bottom: var(--sp-5);
  border-bottom: 1px solid var(--color-neutral-200);
}
.user-manage__kicker {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.3em;
  color: var(--color-neutral-400);
  margin: 0 0 var(--sp-2);
}
.user-manage__hed {
  font-family: var(--font-serif);
  font-size: 44px;
  font-weight: 700;
  line-height: var(--lh-display);
  letter-spacing: var(--tracking-tight);
  color: var(--color-neutral-900);
  margin: 0 0 var(--sp-2);
}
.user-manage__deck {
  margin: 0;
  font-family: var(--font-serif);
  font-style: italic;
  font-size: var(--fs-md);
  color: var(--color-neutral-500);
  line-height: var(--lh-body);
  max-width: 720px;
}

/* ---------- Stats Grid ---------- */
.user-manage__stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--sp-4);
  margin-bottom: var(--sp-6);
}
@media (max-width: 1024px) {
  .user-manage__stats { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 640px) {
  .user-manage__stats { grid-template-columns: 1fr; }
}

.user-stat {
  display: flex;
  align-items: center;
  gap: var(--sp-4);
  padding: var(--sp-5);
  background: var(--color-surface);
  border: 1px solid var(--color-neutral-200);
  border-radius: var(--radius-lg);
  transition: all var(--dur-2) var(--ease);
  animation: fadeInUp 400ms var(--ease) both;
}
.user-stat:nth-child(2) { animation-delay: 50ms; }
.user-stat:nth-child(3) { animation-delay: 100ms; }
.user-stat:nth-child(4) { animation-delay: 150ms; }

.user-stat:hover {
  border-color: var(--color-neutral-300);
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}
.user-stat__icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  background: var(--color-primary-50);
  color: var(--color-primary-600);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.user-stat__icon--role { background: #ede9fe; color: #7c3aed; }
.user-stat__icon--perm { background: #dbeafe; color: #2563eb; }
.user-stat__icon--org { background: #fef3c7; color: #b45309; }

.user-stat__body {
  display: flex;
  flex-direction: column;
  gap: var(--sp-1);
}
.user-stat__num {
  font-family: var(--font-serif);
  font-size: var(--fs-2xl);
  font-weight: 700;
  color: var(--color-neutral-900);
  line-height: var(--lh-tight);
}
.user-stat__label {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.15em;
  color: var(--color-neutral-400);
  text-transform: uppercase;
}

/* ---------- Panel ---------- */
.user-manage__panel {
  background: var(--color-surface);
  border: 1px solid var(--color-neutral-200);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  animation: fadeInUp 400ms var(--ease) both;
  animation-delay: 200ms;
}
.user-manage__panel-inner {
  padding: var(--sp-6);
}
.user-manage__brand {
  display: flex;
  align-items: flex-start;
  gap: var(--sp-4);
}
.user-manage__brand-icon {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, #4f46e5, #6366f1);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 12px 32px rgba(79,70,229,0.25);
}
.user-manage__panel-title {
  margin: 0 0 var(--sp-2);
  font-family: var(--font-serif);
  font-size: var(--fs-xl);
  font-weight: 700;
  color: var(--color-neutral-900);
  line-height: var(--lh-tight);
}
.user-manage__panel-desc {
  margin: 0;
  color: var(--color-neutral-600);
  font-size: var(--fs-sm);
  line-height: var(--lh-body);
}

/* ---------- Panel Footer ---------- */
.user-manage__panel-foot {
  display: flex;
  align-items: center;
  gap: var(--sp-4);
  padding: var(--sp-4) var(--sp-6);
  background: var(--color-neutral-50);
  border-top: 1px solid var(--color-neutral-200);
  flex-wrap: wrap;
}
.user-manage__foot-item {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
}
.user-manage__foot-label {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.1em;
  color: var(--color-neutral-400);
  text-transform: uppercase;
}
.user-manage__foot-value {
  font-size: var(--fs-sm);
  color: var(--color-neutral-700);
  font-weight: 500;
}
.user-manage__foot-sep {
  width: 1px;
  height: 20px;
  background: var(--color-neutral-200);
}

.re-tag__dot {
  width: 6px;
  height: 6px;
  border-radius: var(--radius-full);
  background: currentColor;
  box-shadow: 0 0 0 3px rgba(22,163,74,0.2);
}

/* ---------- Animations ---------- */
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(12px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* ---------- Responsive ---------- */
@media (max-width: 768px) {
  .user-manage__hed { font-size: 32px; }
  .user-manage__brand { flex-direction: column; }
  .user-manage__panel-foot { flex-direction: column; align-items: flex-start; gap: var(--sp-3); }
  .user-manage__foot-sep { display: none; }
}
</style>
