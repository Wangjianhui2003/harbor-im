<script setup>

import {nextTick, ref} from "vue";

const props = defineProps({
  ownerId: {
    type: Number,
  },
  groupMembers: {
    type: Array,
  },
})

const emit = defineEmits(['submit'])


//输入区
const content = ref(null)

const imageList = ref([])
const fileList = ref([])
const currentId = ref(0)
const atSearchText = ref(null)
const compositionFlag = ref(false)
const atIng = ref(false)
const isInputEmpty = ref(true)
const changeStored = ref(true)
const blurRange = ref(null)

//自定义enter
const onKeydown = (e) => {
  //enter
  if(e.keyCode === 13) {
    e.preventDefault()
    e.stopPropagation()
    if(e.ctrlKey){
      //ctrl + enter换行 TODO:不能连续换行bug
      let line = newLine()
      let after = document.createTextNode('\u00A0')
      line.appendChild(after)
      selectElement(line.childNodes[0], 0)
    }else{
      //正在输入不要提交(适配中文输入法)
      if (compositionFlag.value) {
        return
      }
      submit();
    }
    return
  }
  // 删除键
  if (e.keyCode === 8) {
    console.log("delete")
    // 等待dom更新
    setTimeout(() => {
      let s = content.value.innerHTML.trim();
      // 空dom时，需要刷新dom
      console.log(s);
      if (s === '' || s === '<br>' || s === '<div>&nbsp;</div>') {
        // 拼接随机长度的空格，以刷新dom
        empty();
        isInputEmpty.value = true;
        selectElement(content.value);
      } else {
        isInputEmpty.value = false;
      }
    })
  }
}

//
const selectElement = (element,endOffset) => {
  let selection = window.getSelection();
  // 插入元素可能不是立即执行的，vue可能会在插入元素后再更新dom
  nextTick(() => {
    let t1 = document.createRange();
    t1.setStart(element, 0);
    t1.setEnd(element, endOffset || 0);
    if (element.firstChild) {
      t1.selectNodeContents(element.firstChild);
    }
    t1.collapse();
    selection.removeAllRanges();
    selection.addRange(t1);
    // 需要时自动聚焦
    if (element.focus) {
      element.focus();
    }
  })
}

//换行
const newLine = () => {
  let selection = window.getSelection();
  let range = selection.getRangeAt(0);
  let divElement = document.createElement('div');
  let endContainer = range.endContainer;
  let parentElement = endContainer.parentElement;
  if (parentElement.parentElement === content.value) {
    divElement.innerHTML = endContainer.textContent.substring(range.endOffset).trim();
    endContainer.textContent = endContainer.textContent.substring(0, range.endOffset);
    // 插入到当前div（当前行）后面
    parentElement.insertAdjacentElement('afterend', divElement);
  } else {
    divElement.innerHTML = '';
    content.value.append(divElement);
  }
  return divElement;
}

//发送(触发时间，让父组件处理
const submit = () =>{
  console.log(content.value.innerHTML)
  let nodes = content.value.childNodes
  let fullList = [];
  let tempText = '';
  let atUserIds = [];
  let each = (nodes) => {
    for(let i = 0; i < nodes.length; i++) {
      //TODO:处理图片at等数据
      let node = nodes[i]
      if(!node){
        continue;
      }
      //纯文本
      if (node.nodeType === 3) {
        tempText += html2Escape(node.textContent);
        continue;
      }
      let nodeName = node.nodeName.toLowerCase();
      //跳过脚本
      if (nodeName === 'script') {
        continue;
      }else if(nodeName === 'div') {
        tempText += '\n';
        each(node.childNodes);
      }
    }
  }
  each(nodes)
  let text = tempText.trim();
  //文本非空
  if (text !== '') {
    fullList.push({
      type: 'text',
      content: text,
      atUserIds: atUserIds
    })
  }
  console.log("submit",fullList)
  emit('submit',fullList)
}

//转义,防止xss攻击
const html2Escape = (strHtml) => {
  return strHtml.replace(/[<>&"]/g, function (c) {
    return {
      '<': '&lt;',
      '>': '&gt;',
      '&': '&amp;',
      '"': '&quot;'
    }[c];
  });
}

//输入结束时触发
const onCompositionend = (e) => {
  compositionFlag.value = false
  onEditorInput(e)
}

//编辑器输入时
const onEditorInput = (e) => {
  isInputEmpty.value = false
}

//清空
const clear = () =>{
  empty();
  imageList.value = [];
  fileList.value = [];
}

//清空
const empty = () => {
  content.value.innerHTML = "";
  let line = newLine();
  let after = document.createTextNode('\u00A0');
  line.appendChild(after);
  nextTick(() => selectElement(after));
}

//聚焦
const focus = () =>{
  content.value.focus()
}

//暴露方法
defineExpose({
  focus,
  clear
})

</script>

<template>
  <div class="chat-input-area">
    <div class="input"
         contenteditable="true"
         ref="content"
         @keydown="onKeydown"
         @compositionstart="compositionFlag=true"
         @compositionend="onCompositionend"
    >
    </div>
  </div>
</template>

<style scoped lang="scss">

.chat-input-area {
  min-height: 100%;
  width: 100%;
  position: relative;
  background-color: var(--theme-light-gray);

  .input {
    position: absolute;
    left: 11px;
    right: 0;
    bottom: 10px;
    min-height: 100%;
    width: 98%;
    outline: none;
    padding: 8px;
    padding-left: 20px;
    padding-top: 15px;
    border: solid 1px #ddd;
    border-radius: 10px;
    background-color: white;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
  }

}


</style>