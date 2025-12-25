<script setup>
//创建或加入群聊面板

import { reactive, ref } from "vue";
import { createGroup } from "../../api/group.js";
import { useGroupStore } from "../../store/groupStore.js";

const emit = defineEmits(["groupCreated"]);

const props = defineProps({
  addPanelVisible: {
    type: Boolean,
    default: false,
  },
});

const groupStore = useGroupStore();
const searchText = ref("");
const form = reactive({
  name: "",
  headImage: "",
  headImageThumb: "",
  joinType: 0,
});

/**
 * 提交表单
 */
const onSubmit = () => {
  createGroup(form).then((groupVO) => {
    groupStore.addGroup(groupVO);
    emit("groupCreated");
  });
};
</script>

<template>
  <el-dialog
    v-model="props.addPanelVisible"
    :modal="false"
    title="搜索或创建群聊"
  >
    <el-tabs>
      <el-tab-pane label="搜索">
        <el-input v-model="searchText"> </el-input>
      </el-tab-pane>
      <div>
        <el-scrollbar>
          <div></div>
        </el-scrollbar>
      </div>
      <el-tab-pane label="创建">
        <el-form :model="form">
          <el-form-item label="群名称">
            <el-input v-model="form.name"></el-input>
          </el-form-item>
          <el-form-item label="加入类型">
            <el-radio-group v-model="form.joinType">
              <el-radio :value="0">直接加入</el-radio>
              <el-radio :value="1">需要管理员验证</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-button type="primary" @click="onSubmit">创建</el-button>
        </el-form>
      </el-tab-pane>
    </el-tabs>
  </el-dialog>
</template>

<style scoped lang="scss"></style>
