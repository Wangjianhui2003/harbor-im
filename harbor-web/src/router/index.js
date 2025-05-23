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
                path: "/home/chat",
                component: () => import("../view/Chat.vue"),
            },
            {
                name: "Friend",
                path: "/home/friend",
                component: () => import("../view/Friend.vue"),
            },
            {
                name: "GROUP",
                path: "/home/group",
                component: () => import("../view/Group.vue"),
            }
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes,
})

export default router

