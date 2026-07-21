import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'

/* 1) Element Plus 原生样式;2) 在本项目品牌 token 之上覆盖 el 主题;
 * 3) 设计 token;4) 项目自定义组件类;5) legacy(历史样式,逐步内联删除)。
 */
import 'element-plus/dist/index.css'
import './styles/element-theme.css'
import './styles/token.css'
import './styles/components.css'

import App from './App.vue'

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.mount('#app')
