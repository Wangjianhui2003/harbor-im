import { createApp } from 'vue'
import App from './App.vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import router from './router/index.js'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import './assets/style/reset.css'
import './assets/style/default.css'
import {createPinia} from "pinia";
import './assets/style/theme.scss'

const app = createApp(App)
const pinia = createPinia();

app.use(router)
app.use(ElementPlus)
app.use(pinia)

//注册elementplus icon
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}
app.mount('#app')