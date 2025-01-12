<template>
  <ShadcnModal v-model="visible"
               height="410"
               width="50%"
               :title="$t('source.common.filterData')"
               @on-close="onCancel">

    <ShadcnForm v-model="formState">
      <div class="flex items-center space-x-2" v-for="(item, index) in formState.filters">
        <ShadcnFormItem name="column" class="w-full">
          <ShadcnSelect v-model="item.index" @on-change="onFetchOperations(item.index, item)">
            <template #options>
              <ShadcnSelectOption v-for="column in columns" :value="column" :label="column"/>
            </template>
          </ShadcnSelect>
        </ShadcnFormItem>

        <ShadcnFormItem name="operator" class="w-full">
          <ShadcnSelect v-model="item.operator" :disabled="!item.index">
            <template #options>
              <ShadcnSelectOption v-for="operation in item.operations" :value="operation" :label="operation">
                {{ operation }}
              </ShadcnSelectOption>
            </template>
          </ShadcnSelect>
        </ShadcnFormItem>

        <ShadcnFormItem name="value" class="w-full">
          <ShadcnInput v-model="item.value" :disabled="!item.operator"/>
        </ShadcnFormItem>

        <ShadcnFormItem>
          <ShadcnButton circle
                        size="small"
                        type="error"
                        @click="onRemoveFilter(index)">
            <ShadcnIcon icon="Minus"/>
          </ShadcnButton>
        </ShadcnFormItem>
      </div>

      <ShadcnFormItem class="space-y-1" :label="$t('source.common.filterCondition')">
        <ShadcnButton size="small" @click="onAddFilter">
          {{ $t('source.common.addFilter') }}
        </ShadcnButton>
      </ShadcnFormItem>
    </ShadcnForm>

    <template #footer>
      <ShadcnSpace>
        <ShadcnButton type="default" @click="onCancel">
          {{ $t('common.cancel') }}
        </ShadcnButton>
        <ShadcnButton @click="onFilter">
          {{ $t('common.apply') }}
        </ShadcnButton>
      </ShadcnSpace>
    </template>
  </ShadcnModal>
</template>

<script lang="ts">
import { defineComponent } from 'vue'
import { cloneDeep } from 'lodash'

export default defineComponent({
  name: 'TableRowFilter',
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
      type: Boolean
    },
    columns: {
      type: Array,
      default: [] as number[]
    },
    types: {
      type: Array,
      default: [] as number[]
    },
    configure: {
      type: Object as () => any
    }
  },
  created()
  {
    if (this.configure) {
      this.formState = {
        filters: cloneDeep(this.configure)
      }
    }
  },
  data()
  {
    return {
      OPERATOR: '',
      formState: {
        filters: [] as any[]
      }
    }
  },
  methods: {
    onAddFilter()
    {
      const filter = {}
      this.formState.filters.push(filter)
    },
    onRemoveFilter(index: number)
    {
      this.formState.filters.splice(index, 1)
    },
    onFetchOperations(value: any, filter: any)
    {
      filter.name = value
      filter.operations = ['EQ', 'NE', 'GT', 'GE', 'LT', 'LE', 'LIKE', 'IN', 'NOT IN', 'IS NULL', 'IS NOT NULL']
    },
    onFilter()
    {
      this.$emit('apply', this.formState.filters)

      this.onCancel()
    },
    onCancel()
    {
      this.visible = false
    }
  }
})
</script>
