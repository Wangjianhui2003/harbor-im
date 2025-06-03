<script setup>

const props = defineProps({
  placeholder:{
    type: String,
    default: '',
  },
  searchText:{
    type: String,
  },
  type: {
    type: String,
    default: 'search',
  }
})
</script>

<template>
  <input class="search-bar"
         :value="props.searchText"
         :placeholder="props.placeholder"
         @input="$emit('update:searchText',$event.target.value)"
         :type="props.type">
</template>

<style scoped lang="scss">

.search-bar{
  background-color: #464955;
  color: #ffffff;
  padding: .15rem .5rem;
  border-radius: 4px;
  outline: none;
  border: none;
  line-height: 1.15;

  &:focus {
    border-bottom: 2px solid #5e5e5e;
    border-radius: 4px 4px 2px 2px;
  }

  &:hover {
  }
}

</style>