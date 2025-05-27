import {createMemoryHistory, createRouter, createWebHistory} from 'vue-router'
import Login from "../view/Login.vue";
import Home from "../view/Home.vue";
import Register from "../view/Register.vue";

const routes = [
    {
        path: "/",
        redirect: "/login"
    },
    {
        name: "Login",
        path: "/login",
        component: Login
    },
    {
        name: "Register",
        path: "/register",
        component: Register
    },
    {
        name: "Home",
        path: "/home",
        component: Home,
        children: [
            {
                name: "Chat",
                path: "chat",
                component: () => import("../view/Chat.vue"),
            },
            {
                name: "Friend",
                path: "friend",
                component: () => import("../view/Friend.vue"),
            },
            {
                name: "Group",
                path: "group",
                component: () => import("../view/Group.vue"),
            },
            {
                name: "Setting",
                path: "setting",
                component: () => import("../view/Setting.vue"),
            }
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes,
})

export default router

