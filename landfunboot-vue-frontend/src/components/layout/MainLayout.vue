<script setup lang="ts">
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'
</script>

<template>
  <div class="flex min-h-screen w-full">
    <Sidebar />
    <div class="flex flex-col flex-1 min-w-0">
      <Header />
      <main class="flex flex-1 flex-col gap-4 p-5 lg:p-6 overflow-auto" style="background: hsl(var(--background));">
        <router-view />
      </main>
    </div>
  </div>
</template>
