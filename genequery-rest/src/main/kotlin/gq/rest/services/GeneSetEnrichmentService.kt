package gq.rest.services

import gq.core.data.GQGseInfo
import gq.core.data.Species
import gq.core.gea.EnrichmentResultItem
import gq.core.gea.SpecifiedEntrezGenes
import gq.core.gea.findBonferroniSignificant
import gq.rest.GQDataRepository
import gq.rest.config.GQRestProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

data class EnrichmentResponse(val identifiedGeneFormat: String,
                              val geneConversionMap: Map<String, Long?>,
                              val gseToTitle: Map<String, String>,
                              val enrichmentResultItems: List<EnrichmentResultItem>)

@Service
open class GeneSetEnrichmentService @Autowired constructor(
        private val gqRestProperties: GQRestProperties,
        private val gqDataRepository: GQDataRepository) {

    open fun findEnrichedModules(rawGenes: List<String>,
                                 speciesFrom: Species,
                                 speciesTo: Species = speciesFrom): EnrichmentResponse {
        val (identifiedGeneFormat, conversionMap) = convertGenesToEntrez(rawGenes, speciesFrom, speciesTo, gqDataRepository)

        val entrezIds = conversionMap.values.filterNotNull()
        val enrichmentItems = findBonferroniSignificant(
                gqDataRepository.moduleCollection,
                SpecifiedEntrezGenes(speciesTo, entrezIds),
                gqRestProperties.adjPvalueMin)
        val gseToTitle = enrichmentItems.associate {
            Pair(GQGseInfo.idToName(it.gse),
                    gqDataRepository.gseInfoCollection[it.gse]?.title ?: GQGseInfo.DEFAULT_TITLE)
        }
        return EnrichmentResponse(identifiedGeneFormat.formatName, conversionMap, gseToTitle, enrichmentItems)
    }
}