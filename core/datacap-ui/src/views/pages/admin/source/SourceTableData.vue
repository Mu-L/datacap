<template>
  <div class="relative">
    <ShadcnSpin v-model="loading" fixed/>

    <ShadcnCard class="h-screen">
      <template #title>
        <ShadcnSpace class="items-center">
          <ShadcnTooltip :content="$t('source.common.firstPage')">
            <ShadcnButton circle
                          size="small"
                          :disabled="!configure.pagination.hasPrevious"
                          @click="onApplyPagination(configure.operator.FIRST)">
              <ShadcnIcon icon="ArrowLeftToLine" size="15"/>
            </ShadcnButton>
          </ShadcnTooltip>

          <ShadcnTooltip :content="$t('source.common.previousPage')">
            <ShadcnButton circle
                          size="small"
                          :disabled="!configure.pagination.hasPrevious"
                          @click="onApplyPagination(configure.operator.PREVIOUS)">
              <ShadcnIcon icon="ArrowLeft" size="15"/>
            </ShadcnButton>
          </ShadcnTooltip>

          <ShadcnTooltip :content="$t('source.common.nextPage')">
            <ShadcnButton circle
                          size="small"
                          :disabled="!configure.pagination.hasNext"
                          @click="onApplyPagination(configure.operator.NEXT)">
              <ShadcnIcon icon="ArrowRight" size="15"/>
            </ShadcnButton>
          </ShadcnTooltip>

          <ShadcnTooltip :content="$t('source.common.lastPage')">
            <ShadcnButton circle
                          size="small"
                          :disabled="!configure.pagination.hasNext"
                          @click="onApplyPagination(configure.operator.LAST)">
              <ShadcnIcon icon="ArrowRightToLine" size="15"/>
            </ShadcnButton>
          </ShadcnTooltip>

          <ShadcnTooltip :content="$t('user.common.setting')">
            <ShadcnButton circle size="small" @click="visibleSettings(true)">
              <ShadcnIcon icon="Cog" size="15"/>
            </ShadcnButton>
          </ShadcnTooltip>

          <div class="text-sm text-muted-foreground flex gap-2 ml-4 mr-4">
            [ <span>{{ configure.pagination.startIndex }}</span> / <span>{{ configure.pagination.endIndex }}</span> ]
            <span>of</span>
            <span>{{ configure.pagination.total }}</span>
            <span>{{ $t('source.common.records') }}</span>
          </div>

          <ShadcnTooltip :content="$t('source.common.addRows')">
            <ShadcnButton circle size="small" @click="onAddOrCloneRow(false)">
              <ShadcnIcon icon="Plus" size="15"/>
            </ShadcnButton>
          </ShadcnTooltip>

          <ShadcnTooltip :content="$t('source.common.copyRows')">
            <ShadcnButton circle
                          size="small"
                          :disabled="dataSelectedChanged.columns.length === 0"
                          @click="onAddOrCloneRow(true)">
              <ShadcnIcon icon="Copy" size="15"/>
            </ShadcnButton>
          </ShadcnTooltip>

          <ShadcnTooltip :content="$t('source.common.deleteRows')">
            <ShadcnButton circle
                          size="small"
                          :disabled="!dataSelectedChanged.changed"
                          @click="visibleChanged(true)">
              <ShadcnIcon icon="Minus" size="15"/>
            </ShadcnButton>
          </ShadcnTooltip>

          <ShadcnTooltip :content="$t('source.common.previewPendingChanges')">
            <ShadcnButton circle
                          size="small"
                          :disabled="!dataCellChanged.changed && dataCellChanged.columns.length === 0"
                          @click="visibleCellChanged(true)">
              <ShadcnIcon icon="RectangleEllipsis" size="15"/>
            </ShadcnButton>
          </ShadcnTooltip>

          <ShadcnTooltip :content="$t('common.preview')">
            <ShadcnButton circle size="small" @click="visibleContents(true)">
              <ShadcnIcon icon="Eye" size="15"/>
            </ShadcnButton>
          </ShadcnTooltip>

          <ShadcnTooltip :content="$t('common.refresh')">
            <ShadcnButton circle size="small" @click="onRefresh">
              <ShadcnIcon icon="RefreshCw" size="15"/>
            </ShadcnButton>
          </ShadcnTooltip>
        </ShadcnSpace>
      </template>

      <template #extra>
        <ShadcnSpace class="items-center">
          <ShadcnTooltip :content="$t('source.common.visibleColumn')">
            <ShadcnButton circle size="small" @click="visibleColumns(null, true)">
              <ShadcnIcon icon="Columns" size="15"/>
            </ShadcnButton>
          </ShadcnTooltip>

          <ShadcnTooltip :content="$t('source.common.filterData')">
            <ShadcnButton circle size="small" @click="onFilterConfigure(true)">
              <ShadcnIcon icon="Filter" size="15"/>
            </ShadcnButton>
          </ShadcnTooltip>
        </ShadcnSpace>
      </template>

      <div class="relative">
        <ShadcnSpin v-model="refererLoading" fixed/>

        <AgGridVue class="ag-theme-datacap"
                   style="width: 100%; height: calc(100vh - 40px);"
                   :gridOptions="gridOptions"
                   :columnDefs="configure.headers"
                   :rowData="configure.datasets"
                   :tooltipShowDelay="100"
                   :sortingOrder="['desc', 'asc', null]"
                   :rowSelection="'multiple'"
                   @grid-ready="onGridReady"
                   @sortChanged="onSortChanged"
                   @cellValueChanged="onCellValueChanged"
                   @selectionChanged="onSelectionChanged"
                   @columnVisible="onColumnVisible"
                   @columnMoved="onColumnMoved">
        </AgGridVue>
      </div>
    </ShadcnCard>

    <!--    <TableCellInfo v-if="dataCellChanged.pending"-->
    <!--                   :isVisible="dataCellChanged.pending"-->
    <!--                   :columns="dataCellChanged.columns"-->
    <!--                   :type="dataCellChanged.type"-->
    <!--                   @close="visibleCellChanged(false)"/>-->

    <!--    <TableRowDelete v-if="dataSelectedChanged.pending"-->
    <!--                    :isVisible="dataSelectedChanged.pending"-->
    <!--                    :columns="dataSelectedChanged.columns"-->
    <!--                    @close="visibleChanged(false)"/>-->

    <TableColumn v-if="visibleColumn.show"
                 :isVisible="visibleColumn.show"
                 :columns="visibleColumn.columns"
                 @close="visibleColumns($event, false)"
                 @change="visibleColumns($event, false)">
    </TableColumn>

    <TableRowFilter v-if="filterConfigure.show"
                    :isVisible="filterConfigure.show"
                    :columns="filterConfigure.columns"
                    :types="filterConfigure.types"
                    :configure="filterConfigure.filters"
                    @apply="onApplyFilter"
                    @close="onFilterConfigure(false)">
    </TableRowFilter>

    <TablePagination v-if="visibleSetting.show"
                     :is-visible="visibleSetting.show"
                     :pagination="configure.pagination"
                     @close="visibleSettings(false)"
                     @change="configure.pagination = $event">
    </TablePagination>

    <SqlInfo v-if="visibleContent.show"
             :isVisible="visibleContent.show"
             :content="visibleContent.content"
             @close="visibleContents(false)">
    </SqlInfo>
  </div>
</template>

<script lang="ts">
import { defineComponent, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { AgGridVue } from 'ag-grid-vue3'
import 'ag-grid-community/styles/ag-grid.css'
import '@/views/components/grid/ag-theme-datacap.css'
import { ColumnApi, ColumnState, GridApi } from 'ag-grid-community'
import { PaginationEnum, PaginationModel } from '@/model/pagination.ts'
import { createColumnDefs, createDataEditorOptions } from '@/views/pages/admin/source/components/TableUtils.ts'
import { cloneDeep } from 'lodash'
import TableRowDelete from '@/views/pages/admin/source/components/TableRowDelete.vue'
import TableCellInfo from '@/views/pages/admin/source/components/TableCellInfo.vue'
import TableColumn from '@/views/pages/admin/source/components/TableColumn.vue'
import TableRowFilter from '@/views/pages/admin/source/components/TableRowFilter.vue'
import SqlInfo from '@/views/components/sql/SqlInfo.vue'
import TablePagination from '@/views/pages/admin/source/components/TablePagination.vue'
import MetadataService from '@/services/metadata'

export default defineComponent({
  name: 'SourceTableData',
  components: { TablePagination, SqlInfo, TableRowFilter, TableColumn, TableCellInfo, TableRowDelete, AgGridVue },
  created()
  {
    this.i18n = useI18n()
    this.handleInitialize()
    this.watchChange()
  },
  data()
  {
    return {
      i18n: null as any,
      loading: false,
      refererLoading: false,
      gridOptions: null as any,
      gridApi: null as unknown as GridApi,
      gridColumnApi: null as unknown as ColumnApi,
      originalColumns: [] as any[],
      originalDatasets: [] as any[],
      newRows: [] as any[],
      configure: {
        headers: [] as any[],
        columns: [] as any[],
        datasets: [] as any[],
        pagination: null as any,
        operator: PaginationEnum
      },
      visibleContent: {
        show: false,
        content: null as unknown as string
      },
      dataCellChanged: {
        changed: false,
        pending: false,
        type: null as any,
        columns: [] as any[]
      },
      dataSelectedChanged: {
        changed: false,
        pending: false,
        columns: [] as any[]
      },
      visibleColumn: {
        show: false,
        columns: [] as any[]
      },
      visibleSetting: {
        show: false
      },
      filterConfigure: {
        show: false,
        columns: [] as any[],
        types: [] as any[],
        filters: [] as any[]
      }
    }
  },
  methods: {
    handleInitialize()
    {
      this.clearData()
      this.gridOptions = createDataEditorOptions(this.i18n)
      if (!this.configure.pagination) {
        this.configure.pagination = {
          page: 1,
          size: 20
        }
      }

      const code = this.$route?.params.source
      const database = this.$route?.params.database
      const table = this.$route?.params.table

      if (code && database && table) {
        this.loading = true
        const { pagination } = this.configure

        const conf = {
          pagination: pagination
        }

        MetadataService.queryTable(code, database, table, conf)
                       .then(response => {
                         if (response.status && response.data) {
                           this.configure.headers = createColumnDefs(response.data.headers, response.data.types)
                           this.originalColumns = this.configure.headers
                           this.configure.datasets = response.data.columns
                           this.originalDatasets = cloneDeep(response.data.columns)
                           this.configure.pagination = response.data.pagination
                           this.visibleContent.content = response.data.content
                           this.filterConfigure.columns = cloneDeep(response.data.headers)
                           this.filterConfigure.types = cloneDeep(response.data.types)
                         }
                         else {
                           this.$Message.error({
                             content: response.message,
                             showIcon: true
                           })
                         }
                       })
                       .finally(() => this.loading = false)
      }
    },
    handleRefererData(configure: any)
    {
      this.configure.datasets = []
      this.gridOptions.overlayNoRowsTemplate = '<span></span>'
      const code = this.$route?.params.source
      const database = this.$route?.params.database
      const table = this.$route?.params.table

      if (code && database && table) {
        this.refererLoading = true
        MetadataService.queryTable(code, database, table, configure)
                       .then(response => {
                         if (response.status && response.data) {
                           this.configure.headers = createColumnDefs(response.data.headers, response.data.types)
                           this.configure.datasets = response.data.columns
                           this.originalDatasets = cloneDeep(response.data.columns)
                           if (this.configure?.datasets?.length === 0) {
                             this.gridOptions.overlayNoRowsTemplate = '<span>No Rows To Show</span>'
                           }
                           this.configure.pagination = response.data.pagination
                           this.visibleContent.content = response.data.content
                           this.filterConfigure.columns = cloneDeep(response.data.headers)
                           this.filterConfigure.types = cloneDeep(response.data.types)
                         }
                         else {
                           this.$Message.error({
                             content: response.message,
                             showIcon: true
                           })
                         }
                       })
                       .finally(() => this.refererLoading = false)
      }
    },
    onRefresh()
    {
      this.handleRefererData(this.getConfigure())
    },
    onGridReady(params: { api: GridApi; columnApi: ColumnApi; })
    {
      this.gridApi = params.api
      this.gridColumnApi = params.columnApi
    },
    onSortChanged()
    {
      this.handleRefererData(this.getConfigure())
    },
    onCellValueChanged(event: { data: any; colDef: { field: string; }; oldValue: any; newValue: any; rowIndex: number })
    {
      // If the index is less than or equal to the length of the current data collection -1, no request type is specified for the new data
      if (event.rowIndex <= this.originalDatasets.length - 1) {
        const oldColumn = event.data
        const originalColumn = cloneDeep(oldColumn)
        originalColumn[event.colDef.field] = event.oldValue
        this.dataCellChanged.changed = true
        const column: SqlColumn = {
          column: event.colDef.field,
          value: event.newValue,
          original: originalColumn
        }
        this.dataCellChanged.type = SqlType.UPDATE
        this.dataCellChanged.columns.push(column)
      }
    },
    onSelectionChanged()
    {
      const selectedRows = this.gridApi.getSelectedRows()
      this.dataSelectedChanged.changed = true
      this.dataSelectedChanged.columns = selectedRows
    },
    onColumnVisible(event: { visible: any; column: { visible: any; colId: any; }; })
    {
      if (!event.visible) {
        this.configure.headers.map((column: { field: any; checked: boolean; }) => {
          if (column.field === event.column.colId) {
            column.checked = false
          }
        })
      }
    },
    onColumnMoved(event: { finished: any; })
    {
      if (event.finished) {
        this.handleRefererData(this.getConfigure())
      }
    },
    onApplyPagination(operator: PaginationEnum)
    {
      if (this.configure.pagination.page) {
        if (operator === PaginationEnum.PREVIOUS) {
          this.configure.pagination.page--
        }
        else if (operator === PaginationEnum.NEXT) {
          this.configure.pagination.page++
        }
        else if (operator === PaginationEnum.FIRST) {
          this.configure.pagination.page = 1
        }
        else if (operator === PaginationEnum.LAST) {
          this.configure.pagination.page = this.configure.pagination.pages
        }
        this.onSortChanged()
      }
    },
    onApplyFilter(value: any)
    {
      this.filterConfigure.filters = value
    },
    onFilterConfigure(show: boolean)
    {
      this.filterConfigure.show = show
      if (!show) {
        this.handleRefererData(this.getConfigure())
      }
    },
    onAddOrCloneRow(clone: boolean)
    {
      const newData = {} as any
      if (!clone) {
        this.originalColumns.forEach((column: { field: string; }) => {
          newData[column.field] = null
        })
        this.configure.datasets.push(newData)
        this.newRows.push(newData)
      }
      else {
        this.dataSelectedChanged.columns.forEach(column => {
          this.configure.datasets.push(column)
          this.newRows.push(column)
        })
      }
      this.dataCellChanged.type = SqlType.INSERT
      this.dataCellChanged.changed = true
      this.dataCellChanged.columns = this.newRows
      this.gridApi.setRowData(this.configure.datasets)
    },
    visibleCellChanged(isOpen: boolean)
    {
      this.dataCellChanged.pending = isOpen
      if (!isOpen) {
        this.dataCellChanged.changed = false
        this.dataCellChanged.columns = []
      }
    },
    visibleChanged(isOpen: boolean)
    {
      this.dataSelectedChanged.pending = isOpen
      this.dataSelectedChanged.changed = false
      if (!isOpen) {
        this.handleInitialize()
      }
    },
    visibleContents(show: boolean)
    {
      this.visibleContent.show = show
    },
    visibleColumns(event: any, show: boolean)
    {
      this.visibleColumn.show = show
      if (event) {
        const configure = this.getConfigure()
        configure.columns = event.map((item: string) => ({ name: item }))
        // Remove the reduced column is not selected
        this.originalColumns.filter((item: { field: string; }) => !event.includes(item.field))
            .map((item: { checked: boolean; }) => {
              item.checked = false
            })
        // Add new Column is selected
        this.originalColumns.filter((item: { field: string; }) => event.includes(item.field))
            .map((item: { checked: boolean; }) => {
              item.checked = true
            })
        this.handleRefererData(configure)
      }
      this.visibleColumn.columns = this.originalColumns
    },
    visibleSettings(show: boolean)
    {
      this.visibleSetting.show = show
      if (!show) {
        this.handleRefererData(this.getConfigure())
      }
    },
    getSortConfigure(configure: any)
    {
      const columnState = this.gridColumnApi.getColumnState()
      const orders = columnState.filter(item => item.sort !== null)
                                .map((column: ColumnState) => ({
                                  name: column.colId,
                                  order: column.sort
                                }))
      configure.pagination = this.configure.pagination
      configure.orders = orders
    },
    getVisibleColumn(configure: any)
    {
      configure.columns = this.gridColumnApi.getColumnState()
                              .filter(item => !item.hide)
                              .map((item: { colId: any; }) => ({ name: item.colId }))
    },
    getConfigure(): any
    {
      this.clearData()
      const configure = {
        filters: this.filterConfigure.filters
      }
      this.getSortConfigure(configure)
      this.getVisibleColumn(configure)
      return configure
    },
    clearData()
    {
      this.newRows = []
    },
    watchChange()
    {
      watch(
          () => this.$route?.params.table,
          () => {
            this.configure.pagination = null as unknown as PaginationModel
            this.handleInitialize()
          }
      )
    }
  }
})
</script>
