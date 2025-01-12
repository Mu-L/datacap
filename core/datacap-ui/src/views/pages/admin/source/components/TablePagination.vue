<template>
  <ShadcnModal v-model="visible"
               :title="$t('source.common.jumpPage')"
               @on-close="onCancel">
    <ShadcnForm v-model="formState" v-if="formState" @on-submit="onSubmit">
      <ShadcnFormItem name="showPageSize" :label="$t('source.common.showPageSize')">
        <ShadcnNumber v-model="formState.size"
                      :min="1"
                      :max="10000"
                      name="pageSize"/>
      </ShadcnFormItem>

      <ShadcnButton submit type="primary">
        {{ $t('common.apply') }}
      </ShadcnButton>
    </ShadcnForm>
  </ShadcnModal>
</template>

<script lang="ts">
import { defineComponent } from 'vue'

export default defineComponent({
  name: 'TablePagination',
  props: {
    isVisible: {
      type: Boolean
    },
    pagination: {
      type: Object
    }
  },
  computed: {
    visible: {
      get(): boolean
      {
        return this.isVisible
      },
      set(value: boolean)
      {
        this.$emit('close', value)
      }
    }
  },
  data()
  {
    return {
      formState: {
        page: 500
      }
    }
  },
  created()
  {
    if (this.pagination) {
      this.formState = this.pagination
    }
  },
  methods: {
    onCancel()
    {
      this.visible = false
    },
    onSubmit()
    {
      this.visible = false
      this.$emit('change', this.formState)
    }
  }
})
</script>
