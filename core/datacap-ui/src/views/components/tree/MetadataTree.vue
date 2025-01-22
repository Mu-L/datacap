<template>
  <div class="relative overflow-auto" style="height: 500px; max-height: 500px; max-width: 300px;">
    <ShadcnSkeleton v-if="loading" animation class="mt-2"/>

    <ShadcnTree v-else-if="data.length > 0"
                v-model="value"
                :data="data"
                :loadData="onLoadData"
                @on-node-click="onNodeClick">
      <template #label="{ node }">
        <div class="flex items-center space-x-1">
          <ShadcnIcon class="text-xs font-semibold text-gray-500"
                      size="13"
                      :icon="node.level === 2 ? 'Database' :
                              node.level === 3 ? 'Table' :
                              node.level === 4 ? 'Columns' :
                              'Database'
                      ">
          </ShadcnIcon>
          <span class="text-sm font-normal text-gray-500">{{ node.title }}</span>
        </div>
      </template>
    </ShadcnTree>
  </div>
</template>
<script lang="ts">
import { defineComponent, watch } from 'vue'
import { StructureEnum, StructureModel } from '@/model/structure'
import MetadataService from '@/services/metadata.ts'
import { ObjectUtils } from '@/utils/object'

export default defineComponent({
  name: 'MetadataTree',
  props: {
    code: {
      type: String
    }
  },
  data()
  {
    return {
      loading: false,
      value: [],
      data: Array<StructureModel>()
    }
  },
  created()
  {
    this.handleInitialize()
    watch(() => this.code, () => this.handleInitialize())
  },
  methods: {
    handleInitialize()
    {
      if (this.code) {
        this.data = []
        this.loading = true
        MetadataService.getDatabaseBySource(this.code)
                       .then(response => {
                         if (response.status && response.data && response.data.isSuccessful) {
                           response.data.columns.forEach(item => {
                             const structure = {
                               title: item.object_name,
                               catalog: item.object_name,
                               code: item.object_name,
                               level: StructureEnum.DATABASE,
                               value: item.object_name,
                               isLeaf: false
                             }
                             this.data.push(structure)
                           })
                         }
                         else {
                           this.$Message.error({
                             content: response.data.message,
                             showIcon: true
                           })
                         }
                       })
                       .finally(() => this.loading = false)
      }
    },
    onLoadData(item: any, callback: any)
    {
      const children = [] as StructureModel[]
      if (item.level === StructureEnum.DATABASE) {
        MetadataService.getTablesByDatabase(this.code, item.code)
                       .then(response => {
                         if (response.status && response.data && response.data.isSuccessful) {
                           response.data.columns.forEach((value: any) => children.push({
                             title: value.object_name,
                             value: value.object_name,
                             code: value.object_name,
                             database: item.code,
                             catalog: value.object_name,
                             level: StructureEnum.TABLE,
                             isLeaf: false
                           }))
                         }
                         else {
                           this.$Message.error({
                             content: response.data.message,
                             showIcon: true
                           })
                         }
                       })
                       .finally(() => callback(children))
      }
      else if (item.level === StructureEnum.TABLE) {
        MetadataService.getColumnsByTable(this.code, item.database, item.code)
                       .then(response => {
                         if (response.status && response.data && response.data.isSuccessful) {
                           response.data.columns
                                   .filter(value => value.type_name === 'column')
                                   .forEach((value: any) => children.push({
                                     title: value.object_name,
                                     value: value.object_name,
                                     database: item.database,
                                     table: item.code,
                                     catalog: value.object_name,
                                     code: value.object_name,
                                     level: StructureEnum.COLUMN
                                   }))
                         }
                         else {
                           this.$Message.error({
                             content: response.data.message,
                             showIcon: true
                           })
                         }
                       })
                       .finally(() => callback(children))
      }
      else {
        callback(children)
      }
    },
    onNodeClick(item: StructureModel)
    {
      let text: string = item.title as string
      switch (item.level) {
        case StructureEnum.TABLE:
          text = item.database + '.' + text
          break
        case StructureEnum.COLUMN:
          text = item.database + '.' + item.table + '.' + text
          break
      }
      ObjectUtils.copy(text)
    }
  }
})
</script>
