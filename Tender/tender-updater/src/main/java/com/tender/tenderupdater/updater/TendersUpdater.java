package com.tender.tenderupdater.updater;

import com.tender.tenderclient.client.ITendersClient;
import com.tender.tenderclient.client.data.*;
import com.tender.tenderdatabase.entity.*;
import com.tender.tenderdatabase.repositories.ICatalogData;
import com.tender.tenderupdater.mappers.ICatalogMappers;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class TendersUpdater implements IUpdateTenders{

    private final ICatalogData catalog;
    private final ITendersClient client;
    private final ICatalogMappers mappers;

    public TendersUpdater(ICatalogData catalog, ITendersClient client, ICatalogMappers mappers) {
        this.catalog = catalog;
        this.client = client;
        this.mappers = mappers;
    }

    @Override
    public void UpdateByPage(int page) {
        List<Long> sourceId = getSourceIds(page);

        List<TenderDto> TendersDto = sourceId.stream().map(client::getTender).toList();
        List<PurchaserDto> PurchaserDto = TendersDto.stream().map(this::getPurchaserFromTender).toList();
        List<TypeDto> TypeDto = TendersDto.stream().map(this::getTypeFromTender).toList();
        List<AwardDto> AwardDto = TendersDto.stream().map(this::getAwardFromTender).toList();
        List<SupplierDto> SupplierDto = getUniqueSuppliers(AwardDto);

        List<Tender> TendersSave = TendersDto.stream().map(tender -> mappers.forTender().map(tender)).toList();
        List<Purchaser> PurchaserSave = PurchaserDto.stream().map(purchaser -> mappers.forPurchaser().map(purchaser)).toList();
        List<Type> TypeSave = TypeDto.stream().map(type -> mappers.forType().map(type)).toList();
        List<Awarded> AwardSave = AwardDto.stream().map(award -> mappers.forAwarded().map(award)).toList();
        List<Supplier> SupplierSave = SupplierDto.stream().map(supplier -> mappers.forSupplier().map(supplier)).toList();

        setConnections(TendersSave,PurchaserSave,AwardSave,TypeSave,SupplierSave);

        catalog.getTenders().saveAll(TendersSave);
        catalog.getPurchers().saveAll(PurchaserSave);
        catalog.getTypes().saveAll(TypeSave);
        catalog.getAwarded().saveAll(AwardSave);
        catalog.getSupplier().saveAll(SupplierSave);
    }

    private List<Long> getSourceIds(int page){
        List<Long> ids = new ArrayList<>();
        var result = client.getTenders(page);
        ids.addAll(result.tenders().stream().map(TenderDto::id).toList());
        List<Integer> sourceIdIgnore = catalog.getTenders().findAllSourceIds();
        for (Integer id: sourceIdIgnore){
            ids.remove((long) id);
        }
        return ids;
    }

    private PurchaserDto getPurchaserFromTender(TenderDto tender){
        PurchaserDto res = tender.purchaser();
        return new PurchaserDto(tender.id(), res.id(),res.sid(), res.name());
    }

    private TypeDto getTypeFromTender(TenderDto tender){
        TypeDto res = tender.type();
        return new TypeDto(tender.id(), res.id(), res.name(), res.slug());
    }

    private AwardDto getAwardFromTender(TenderDto tender){
        List<AwardDto> res = tender.awarded();
        List<AwardDto> res_f = new ArrayList<>();

        for (AwardDto award: res){
            res_f.add(new AwardDto(tender.id(), award.date(), award.valueForOne(), award.valueForTwo(), award.valueForThree(), award.suppliersId(), award.count(), award.offersCount(), award.value(), award.suppliers()));
        }

        return res_f.get(0);
    }
    private SupplierDto getSupplierFromAwarded(AwardDto awarded){
        return awarded.suppliers().get(0);
    }

    private List<SupplierDto> getUniqueSuppliers(List<AwardDto> awardDtoList) {
        // Получаем идентификаторы существующих поставщиков из репозитория
        List<Long> existingSupplierIds = catalog.getSupplier().findAllSourceIds();

        // Извлекаем всех поставщиков из списка AwardDto
        List<SupplierDto> allSuppliers = awardDtoList.stream()
                .map(this::getSupplierFromAwarded)
                .filter(supplier -> !existingSupplierIds.contains(supplier.id())) // Фильтруем только новые поставщики
                .collect(Collectors.toList());

        // Убираем дубликаты из списка новых поставщиков
        Set<Long> seenIds = new HashSet<>();
        return allSuppliers.stream()
                .filter(supplier -> seenIds.add(supplier.id()))
                .collect(Collectors.toList());
    }

    private void setConnections(List<Tender> TendersSave, List<Purchaser> PurchaserSave, List<Awarded> AwardSave, List<Type> TypeSave, List<Supplier> SupplierSave){
        Map<Long, Tender> tenderBySourceId = TendersSave.stream()
                .collect(Collectors.toMap(
                        tender -> (long) tender.getSourceId(),
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        for (Purchaser purchaser : PurchaserSave) {
            Tender tender = tenderBySourceId.get(purchaser.getTender_src_id());
            purchaser.setTender(tender);
        }

        for (Type type : TypeSave) {
            Tender tender = tenderBySourceId.get(type.getTender_src_id());
            type.setTender(tender);
        }

        for (Awarded award : AwardSave) {
            Tender tender = tenderBySourceId.get(award.getTender_src_id());
            award.setTender(tender);
        }

        for (Awarded award: AwardSave){
            for (Supplier supplier: SupplierSave){
                if (award.getSuppliersId() == supplier.getSource_id()){
                    award.setSupplier(supplier);
                }
            }
        }
    }
}
