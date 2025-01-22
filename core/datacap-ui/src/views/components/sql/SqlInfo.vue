<template>
  <ShadcnModal v-model="visible" title="Show Content" width="40%">
    <ShadcnCodeEditor v-model="content"
                      :config="{
                        language: 'sql',
                        readOnly: true,
                        minimap: {
                          enabled: false
                        }
                      }">
    </ShadcnCodeEditor>

    <template #footer>
      <ShadcnButton type="error" @click="onCancel">
        {{ $t('common.cancel') }}
      </ShadcnButton>
    </template>
  </ShadcnModal>
</template>

<script lang="ts">
import { defineComponent, PropType } from 'vue'

export default defineComponent({
  name: 'SqlInfo',
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
  props: {
    isVisible: {
      type: Boolean,
      default: () => false
    },
    content: {
      type: String as PropType<string | null>
    }
  },
  methods: {
    onCancel()
    {
      this.visible = false
    }
  }
})
</script>
