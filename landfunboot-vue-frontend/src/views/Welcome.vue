<script setup lang="ts">
import { computed } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { useRouter } from 'vue-router'

const { user, permissions, hasPermission } = useAuth()
const router = useRouter()

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 12) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const quickLinks = computed(() => {
  const links = [
    { title: '用户管理', desc: '管理系统用户账号和权限', icon: '👤', path: '/system/user', perm: 'sys:user:list', color: '#3b82f6' },
    { title: '角色管理', desc: '配置角色权限和数据范围', icon: '🎭', path: '/system/role', perm: 'sys:role:list', color: '#8b5cf6' },
    { title: '部门管理', desc: '维护组织架构和部门信息', icon: '🏢', path: '/system/dept', perm: 'sys:dept:list', color: '#10b981' },
    { title: '菜单管理', desc: '配置系统菜单和按钮权限', icon: '📋', path: '/system/menu', perm: 'sys:menu:list', color: '#f59e0b' },
    { title: '服务监控', desc: '实时查看服务运行状态', icon: '📊', path: '/monitor', perm: 'sys:monitor:info', color: '#ef4444' },
    { title: '在线用户', desc: '查看并管理在线用户会话', icon: '🟢', path: '/monitor/online', perm: 'sys:user:online', color: '#06b6d4' },
  ]
  return links.filter(l => hasPermission(l.perm))
})
</script>

<template>
  <div class="welcome-page">
    <!-- Hero Section -->
    <div class="hero-section">
      <div class="hero-bg-blob blob-1"></div>
      <div class="hero-bg-blob blob-2"></div>

      <div class="hero-content">
        <div class="avatar-ring">
          <div class="avatar-inner">
            {{ user?.username?.charAt(0).toUpperCase() ?? 'U' }}
          </div>
        </div>
        <h1 class="hero-greeting">{{ greeting }}，{{ user?.username }} 👋</h1>
        <p class="hero-subtitle">欢迎回到 Landfun Boot 企业管理平台</p>
        <div class="hero-badge" v-if="user?.isSuperuser">
          <span class="badge-dot"></span>超级管理员
        </div>
      </div>
    </div>

    <!-- Stats Row -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon" style="background: #eff6ff; color: #3b82f6;">🔑</div>
        <div class="stat-info">
          <div class="stat-value">{{ permissions.length }}</div>
          <div class="stat-label">拥有权限</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #f0fdf4; color: #10b981;">📦</div>
        <div class="stat-info">
          <div class="stat-value">{{ quickLinks.length }}</div>
          <div class="stat-label">可用功能</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #fdf4ff; color: #a855f7;">🏷️</div>
        <div class="stat-info">
          <div class="stat-value">{{ new Date().toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' }) }}</div>
          <div class="stat-label">今天日期</div>
        </div>
      </div>
    </div>

    <!-- Quick Links -->
    <div class="section" v-if="quickLinks.length > 0">
      <div class="section-header">
        <h2 class="section-title">快速导航</h2>
        <p class="section-desc">点击前往您有权访问的功能模块</p>
      </div>
      <div class="links-grid">
        <div
          v-for="link in quickLinks"
          :key="link.path"
          class="link-card"
          @click="router.push(link.path)"
        >
          <div class="link-icon" :style="{ background: link.color + '18', color: link.color }">
            {{ link.icon }}
          </div>
          <div class="link-body">
            <div class="link-title">{{ link.title }}</div>
            <div class="link-desc">{{ link.desc }}</div>
          </div>
          <div class="link-arrow">→</div>
        </div>
      </div>
    </div>

    <!-- No permissions notice -->
    <div class="empty-state" v-if="quickLinks.length === 0">
      <div class="empty-icon">🔒</div>
      <div class="empty-title">暂无可用功能</div>
      <div class="empty-desc">您的账号尚未被分配功能权限，请联系管理员进行配置。</div>
    </div>

    <!-- Footer -->
    <div class="page-footer">
      © 2025 Landfun Technology · 企业管理平台 v1.0.0
    </div>
  </div>
</template>

<style scoped>
.welcome-page {
  min-height: 100%;
  padding: 0 0 40px 0;
  background: hsl(210, 20%, 97%);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

/* Hero */
.hero-section {
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, hsl(220, 70%, 18%) 0%, hsl(240, 60%, 30%) 100%);
  padding: 52px 40px 48px;
  color: white;
}
.hero-bg-blob {
  position: absolute;
  border-radius: 50%;
  opacity: 0.08;
}
.blob-1 {
  width: 400px; height: 400px;
  background: white;
  top: -150px; right: -100px;
}
.blob-2 {
  width: 250px; height: 250px;
  background: hsl(200, 100%, 70%);
  bottom: -80px; left: 40px;
}
.hero-content {
  position: relative;
  z-index: 1;
}
.avatar-ring {
  width: 64px; height: 64px;
  border-radius: 50%;
  background: rgba(255,255,255,0.15);
  border: 2px solid rgba(255,255,255,0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}
.avatar-inner {
  width: 52px; height: 52px;
  border-radius: 50%;
  background: rgba(255,255,255,0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 700;
  color: white;
}
.hero-greeting {
  font-size: 26px;
  font-weight: 700;
  margin: 0 0 8px 0;
  color: white;
}
.hero-subtitle {
  font-size: 14px;
  color: rgba(255,255,255,0.7);
  margin: 0 0 16px 0;
}
.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: rgba(255,255,255,0.12);
  border: 1px solid rgba(255,255,255,0.2);
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12.5px;
  color: rgba(255,255,255,0.9);
}
.badge-dot {
  width: 6px; height: 6px;
  border-radius: 50%;
  background: #4ade80;
}

/* Stats */
.stats-row {
  display: flex;
  gap: 16px;
  padding: 24px 40px 0;
}
.stat-card {
  flex: 1;
  background: white;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  border: 1px solid hsl(210, 20%, 92%);
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
}
.stat-icon {
  width: 44px; height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}
.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: hsl(215, 25%, 18%);
  line-height: 1;
  margin-bottom: 4px;
}
.stat-label {
  font-size: 12px;
  color: hsl(215, 15%, 55%);
}

/* Section */
.section {
  padding: 28px 40px 0;
}
.section-header {
  margin-bottom: 16px;
}
.section-title {
  font-size: 16px;
  font-weight: 600;
  color: hsl(215, 25%, 18%);
  margin: 0 0 4px 0;
}
.section-desc {
  font-size: 13px;
  color: hsl(215, 15%, 55%);
  margin: 0;
}

/* Links Grid */
.links-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
}
.link-card {
  background: white;
  border-radius: 12px;
  border: 1px solid hsl(210, 20%, 92%);
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  cursor: pointer;
  transition: all 0.18s ease;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}
.link-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0,0,0,0.08);
  border-color: hsl(210, 40%, 80%);
}
.link-icon {
  width: 42px; height: 42px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}
.link-body {
  flex: 1;
  min-width: 0;
}
.link-title {
  font-size: 14px;
  font-weight: 600;
  color: hsl(215, 25%, 18%);
  margin-bottom: 3px;
}
.link-desc {
  font-size: 12px;
  color: hsl(215, 15%, 55%);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.link-arrow {
  font-size: 16px;
  color: hsl(215, 15%, 70%);
  flex-shrink: 0;
  transition: transform 0.18s;
}
.link-card:hover .link-arrow {
  transform: translateX(3px);
  color: hsl(215, 50%, 50%);
}

/* Empty State */
.empty-state {
  text-align: center;
  padding: 64px 40px;
}
.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}
.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: hsl(215, 25%, 22%);
  margin-bottom: 8px;
}
.empty-desc {
  font-size: 14px;
  color: hsl(215, 15%, 55%);
  max-width: 360px;
  margin: 0 auto;
  line-height: 1.6;
}

/* Footer */
.page-footer {
  text-align: center;
  padding: 32px 40px 0;
  font-size: 12px;
  color: hsl(215, 15%, 65%);
}
</style>
