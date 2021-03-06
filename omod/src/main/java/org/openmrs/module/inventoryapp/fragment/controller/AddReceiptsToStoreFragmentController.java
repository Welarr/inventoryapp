package org.openmrs.module.inventoryapp.fragment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.openmrs.Role;
import org.openmrs.api.context.Context;
import org.openmrs.module.hospitalcore.InventoryCommonService;
import org.openmrs.module.hospitalcore.model.*;
import org.openmrs.module.hospitalcore.util.ActionValue;
import org.openmrs.module.inventory.InventoryService;
import org.openmrs.module.inventory.model.InventoryStoreDrug;
import org.openmrs.module.inventory.util.DateUtils;
import org.openmrs.module.inventoryapp.global.StoreSingleton;
import org.openmrs.module.inventoryapp.model.DrugInformation;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.UiUtils;
import org.springframework.web.bind.annotation.RequestParam;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by franqq on 3/21/16.
 */
public class AddReceiptsToStoreFragmentController {
    public List<SimpleObject> fetchDrugNames(@RequestParam(value = "categoryId") int categoryId, UiUtils uiUtils)
    {
        List<SimpleObject> drugNames = null;
        InventoryService inventoryService = (InventoryService) Context.getService(InventoryService.class);
        if(categoryId > 0){
            List<InventoryDrug> drugs = inventoryService.findDrug(categoryId, null);
            drugNames = SimpleObject.fromCollection(drugs,uiUtils,"id","name");
        }
        return drugNames;
    }

    public List<SimpleObject> getFormulationByDrugName(@RequestParam(value="drugName") String drugName,UiUtils ui)
    {

        InventoryCommonService inventoryCommonService = (InventoryCommonService) Context.getService(InventoryCommonService.class);
        InventoryDrug drug = inventoryCommonService.getDrugByName(drugName);

        List<SimpleObject> formulationsList = null;

        if(drug != null){
            List<InventoryDrugFormulation> formulations = new ArrayList<InventoryDrugFormulation>(drug.getFormulations());
            formulationsList = SimpleObject.fromCollection(formulations, ui, "id", "name", "dozage");
        }

        return formulationsList;
    }

    public List<DrugInformation> getPrescriptions(String json){
        ObjectMapper mapper = new ObjectMapper();
        List<DrugInformation> list = null;
        try {
            list = mapper.readValue(json,
                    TypeFactory.defaultInstance().constructCollectionType(List.class,
                            DrugInformation.class));

        } catch (IOException e) {
            e.printStackTrace();
        }
        return  list;
    }

    public void saveReceipt( @RequestParam(value = "drugOrder", required = false) String drugOrder,
                             @RequestParam(value = "description", required = false) String description
                             ) throws ParseException {

        List<DrugInformation> drugInformationList = getPrescriptions(drugOrder);
        DrugInformation drugInformation = drugInformationList.get(0);

        int formulation=drugInformation.getDrugFormulationId(), drugId=drugInformation.getDrugId(), quantity=drugInformation.getQuantity();
        String  unitPriceStr=drugInformation.getUnitPrice(),costToPatientStr=drugInformation.getCostToThePatient(), batchNo=drugInformation.getBatchNo(),
                 receiptFrom=drugInformation.getReceiptFrom(), dateManufacture=drugInformation.getDateOfManufacture(), dateExpiry=drugInformation.getDateOfExpiry(), receiptDate=drugInformation.getReceiptDate();


        List<String> errors = new ArrayList<String>();
        InventoryDrug drug=null;
        InventoryService inventoryService = (InventoryService) Context.getService(InventoryService.class);
        List<InventoryDrugCategory> listCategory = inventoryService.findDrugCategory("");
        drug=inventoryService.getDrugById(drugId);

        if(drug == null){
            errors.add("inventory.receiptDrug.drug.required");
        }

        BigDecimal unitPrice  = new BigDecimal(0);

        BigDecimal costToPatient = NumberUtils.createBigDecimal(costToPatientStr);
        if(null!=unitPriceStr && ""!=unitPriceStr)
        {
            unitPrice =  NumberUtils.createBigDecimal(unitPriceStr);
        }

        if(!StringUtils.isBlank(dateManufacture)){
            DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

            Date dateManufac = dateFormatter.parse(dateManufacture);
            Date dateExpi = dateFormatter.parse(dateExpiry);
            if(dateManufac.after(dateExpi)  ){
                errors.add("inventory.receiptDrug.manufacNeedLessThanExpiry");
            }
        }

        InventoryDrugFormulation formulationO = inventoryService.getDrugFormulationById(formulation);
        if(formulationO == null)
        {
            errors.add("inventory.receiptDrug.formulation.required");
        }
        //InventoryDrug drug = inventoryService.getDrugById(drugId);

        if(formulationO != null && drug != null && !drug.getFormulations().contains(formulationO))
        {
            errors.add("inventory.receiptDrug.formulation.notCorrect");
        }

        InventoryStoreDrugTransactionDetail transactionDetail = new InventoryStoreDrugTransactionDetail();
        transactionDetail.setDrug(drug);
        transactionDetail.setAttribute(drug.getAttributeName());
        transactionDetail.setReorderPoint(drug.getReorderQty());
        transactionDetail.setFormulation(inventoryService.getDrugFormulationById(formulation));
        transactionDetail.setBatchNo(batchNo);
        transactionDetail.setCurrentQuantity(quantity);
        transactionDetail.setQuantity(quantity);
        transactionDetail.setUnitPrice(unitPrice);
        transactionDetail.setCostToPatient(costToPatient);
        transactionDetail.setIssueQuantity(0);
        transactionDetail.setCreatedOn(new Date());
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            transactionDetail.setDateExpiry(formatter.parse(dateExpiry+" 23:59:59"));
        }  catch (ParseException e) {
            e.printStackTrace();
        }
        transactionDetail.setDateManufacture(DateUtils.getDateFromStr(dateManufacture));
        transactionDetail.setReceiptDate(DateUtils.getDateFromStr(receiptDate));

        //Sagar Bele : Date - 22-01-2013 Issue Number 660 : [Inventory] Add receipt from field in Table and front end
        transactionDetail.setReceiptFrom(receiptFrom);

		/*Money moneyUnitPrice = new Money(unitPrice);
		Money totl = moneyUnitPrice.times(quantity);
		totl = totl.plus(totl.times((double)VAT/100));
		transactionDetail.setTotalPrice(totl.getAmount());*/

        BigDecimal moneyUnitPrice = costToPatient.multiply(new BigDecimal(quantity));
        //moneyUnitPrice = moneyUnitPrice.add(moneyUnitPrice.multiply(VAT.divide(new BigDecimal(100))));
        transactionDetail.setTotalPrice(moneyUnitPrice);

        int userId = Context.getAuthenticatedUser().getId();
        String fowardParam = "reipt_"+userId;

        List<InventoryStoreDrugTransactionDetail> list = (List<InventoryStoreDrugTransactionDetail> )StoreSingleton.getInstance().getHash().get(fowardParam);
        if(list == null){
            list = new ArrayList<InventoryStoreDrugTransactionDetail>();
        }
        list.add(transactionDetail);
        StoreSingleton.getInstance().getHash().put(fowardParam, list);



        Date date = new Date();
        //	InventoryStore store = inventoryService.getStoreByCollectionRole(new ArrayList<Role>(Context.getAuthenticatedUser().getAllRoles()));;
        List <Role>role=new ArrayList<Role>(Context.getAuthenticatedUser().getAllRoles());

        InventoryStoreRoleRelation srl=null;
        Role rl = null;
        for(Role r: role){
            if(inventoryService.getStoreRoleByName(r.toString())!=null){
                srl = inventoryService.getStoreRoleByName(r.toString());
                rl=r;
            }
        }
        InventoryStore store =null;
        if(srl!=null){
            store = inventoryService.getStoreById(srl.getStoreid());

        }
        InventoryStoreDrugTransaction transaction = new InventoryStoreDrugTransaction();
        transaction.setDescription(description);
        transaction.setCreatedOn(date);
        transaction.setStore(store);
        transaction.setTypeTransaction(ActionValue.TRANSACTION[0]);
        transaction.setCreatedBy(Context.getAuthenticatedUser().getGivenName());
        transaction = inventoryService.saveStoreDrugTransaction(transaction);


        if(drugInformationList != null && drugInformationList.size() > 0){
            for(int i=0;i< list.size();i++){
                InventoryStoreDrugTransactionDetail transactionSDetail = list.get(i);
                //save total first
                //System.out.println("transactionDetail.getDrug(): "+transactionDetail.getDrug());
                //System.out.println("transactionDetail.getFormulation(): "+transactionDetail.getFormulation());
                InventoryStoreDrug storeDrug = inventoryService.getStoreDrug(store.getId(), transactionSDetail.getDrug().getId(), transactionSDetail.getFormulation().getId());
                if(storeDrug == null){
                    storeDrug = new InventoryStoreDrug();
                    storeDrug.setCurrentQuantity(transactionSDetail.getQuantity());
                    storeDrug.setReceiptQuantity(transactionSDetail.getQuantity());
                    storeDrug.setDrug(transactionSDetail.getDrug());
                    storeDrug.setFormulation(transactionSDetail.getFormulation());
                    storeDrug.setStore(store);
                    storeDrug.setStatusIndent(0);
                    storeDrug.setReorderQty(0);
                    storeDrug.setOpeningBalance(0);
                    storeDrug.setClosingBalance(transactionSDetail.getQuantity());
                    storeDrug.setStatus(0);
                    storeDrug.setReorderQty(transactionSDetail.getDrug().getReorderQty());
                    storeDrug = inventoryService.saveStoreDrug(storeDrug);

                }else{
                    storeDrug.setOpeningBalance(storeDrug.getClosingBalance());
                    storeDrug.setClosingBalance(storeDrug.getClosingBalance()+transactionSDetail.getQuantity());
                    storeDrug.setCurrentQuantity(storeDrug.getCurrentQuantity() + transactionSDetail.getQuantity());
                    storeDrug.setReceiptQuantity(transactionSDetail.getQuantity());
                    storeDrug.setReorderQty(transactionSDetail.getDrug().getReorderQty());
                    storeDrug = inventoryService.saveStoreDrug(storeDrug);
                }
                //save transactionDetail
                transactionSDetail.setOpeningBalance(storeDrug.getOpeningBalance());
                transactionSDetail.setClosingBalance(storeDrug.getClosingBalance());
                transactionSDetail.setTransaction(transaction);
                inventoryService.saveStoreDrugTransactionDetail(transactionSDetail);
            }
            StoreSingleton.getInstance().getHash().remove(fowardParam);
        }
    }
}
