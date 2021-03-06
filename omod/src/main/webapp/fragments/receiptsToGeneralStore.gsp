<%
	ui.includeJavascript("billingui", "moment.js")
%>

<script>
    var receiptsDataString;
	
    jq(function () {
        var receiptsData = getReceitsToGeneralStore();
        function ReceiptsDataListView() {
            var self = this;
            // Editable data
            self.receiptsDataItems = ko.observableArray([]);
            var mappedReceiptsDataItems = jQuery.map(receiptsData, function (item) {
                return item;
            });

            self.viewDetails = function (item) {
                window.location.href = emr.pageLink("inventoryapp", "detailedReceiptOfDrug", {
					receiptId: item.id
				});
            }
            self.receiptsDataItems(mappedReceiptsDataItems);

        }
		
		function updateTable(){
			receiptsData = getReceitsToGeneralStore(jq('#receiptName').val().trim(), moment(jq('#rcptFrom-field').val()).format('DD/MM/YYYY'), moment(jq('#rcptDate-field').val()).format('DD/MM/YYYY'));
			list.receiptsDataItems(receiptsData);
		}

		
		jq('#receiptName').on('keyup',function(){
			updateTable();
		});
		
		jq('#rcptFrom').on('change',function(){
			updateTable();
		});
		
		jq('#rcptDate').on('change',function(){
			console.log('Task');
			updateTable();
		});

        var list = new ReceiptsDataListView();
        ko.applyBindings(list, jq("#receiptslist")[0]);
		//console.log(list.receiptsDataItems().);
    });
	
    function getReceitsToGeneralStore(receiptName, fromDate, toDate) {
        var toReturn;
        jQuery.ajax({
            type: "GET",
            url: '${ui.actionLink("inventoryapp", "receiptsToGeneralStore", "fetchReceiptsToGeneralStore")}',
            dataType: "json",
            global: false,
            async: false,
            data: {
                receiptName: receiptName,
                fromDate: fromDate,
                toDate: toDate
            },
            success: function (data) {
                toReturn = data;
            }
        });
        return toReturn;
    }
</script>

<style>
	
</style>

<div class="dashboard clear">
	<div class="info-section">
		<div class="info-header">
			<i class="icon-list-ul"></i>
            <h3>RECEIPT DRUGS</h3>
			
			<div style="margin-top: -1px">
				<i class="icon-filter" style="font-size: 26px!important; color: #5b57a6"></i>
				
				<label for="receiptName">Description</label>
				<input type="text" id="receiptName" name="receiptName" placeholder="Receipt Name" class="searchFieldBlur" title="Receipt Name" style="width: 160px"/>
				<label>&nbsp;&nbsp;From&nbsp;</label>${ui.includeFragment("uicommons", "field/datetimepicker", [formFieldName: 'rFromDate', id: 'rcptFrom', label: '', useTime: false, defaultToday: false, class: ['newdtp']])}
				<label>&nbsp;&nbsp;To&nbsp;</label  >${ui.includeFragment("uicommons", "field/datetimepicker", [formFieldName: 'toDate',    id: 'rcptDate',   label: '', useTime: false, defaultToday: false, class: ['newdtp']])}
			</div>
			
			
		</div>
	</div>
</div>

<table id="receiptslist">
    <thead>
		<th>#</th>
		<th>DATE CREATED</th>
		<th>DESCRIPTION</th>
		<th>STORE</th>
		<th>Notes</th>
    </thead>
    <tbody data-bind="foreach: receiptsDataItems">
		<td data-bind="text: \$index() + 1"></td>
		<td data-bind="text: (createdOn).substring(0, 11).replaceAt(2, ',').replaceAt(6, ' ').insertAt(3, 0, ' ') "></td>
		<td><a data-bind="html:description ,click:\$parent.viewDetails"></a></td>
		<td data-bind="text:store.name"></td>
		<td>N/A</td>
    </tbody>
</table>

<div class="summary-div">
	<a class="button task" href="addReceiptsToGeneralStore.page" style="float: right; margin-top: 5px;width: 160px; text-align: center; height: 20px; padding-top: 15px"><i class="icon-plus small"> </i>&nbsp;Add Receipts</a>
</div>

