package com.hyperion.planning.db;

import com.hyperion.calcmgr.beans.RuleBean;
import com.hyperion.calcmgr.groovy.GroovyInstance;
import com.hyperion.calcmgr.importer.MbrBlock;
import com.hyperion.planning.*;
import com.hyperion.planning.adf.decisionPackages.utils.DPBRDefUpdateUtil;
import com.hyperion.planning.sql.HspExternalServer;
import com.hyperion.planning.sql.keydef.AttributeReferenceMemberIdKeyDef;
import com.hyperion.planning.sql.keydef.DPMemberAttributeKeyDef;
import com.hyperion.planning.sql.keydef.HspDPCubeMappingKeyDef;
import com.hyperion.planning.utils.HspObjectPositioner;
import com.hyperion.planning.datavalidation.HspDVPMRule;
import com.hyperion.planning.datavalidation.HspDVCond;
import com.hyperion.planning.datavalidation.HspDVRule;
import com.hyperion.planning.mf.*;

import com.hyperion.planning.olap.HspOLAP;
import com.hyperion.planning.olap.HspFormCell;
import com.hyperion.planning.event.*;
import com.hyperion.planning.governor.HspCompositeGovernor;
import com.hyperion.planning.governor.HspDimensionGovernor;
import com.hyperion.planning.governor.HspGovernor;
import com.hyperion.planning.governor.HspGovernorThresholdException;
import com.hyperion.planning.governor.HspPMEnabledDimensionGovernor;

import oracle.epm.api.model.DynamicChildStrategy;

import com.hyperion.planning.configurator.HspDiffUtil;
import com.hyperion.planning.governor.HspHealthCheckCriteriaFactory;
import com.hyperion.planning.governor.HspMembersAcrossAllDimensionsGovernor;
import com.hyperion.planning.groovy.HspPostMemberSaveCallbackEventHandler;
import com.hyperion.planning.groovy.MemberChangedEvent;

import oracle.epm.api.model.PlanningGroovyExecutor;

import com.hyperion.planning.metadata.HspMDConstants;
import com.hyperion.planning.sql.*;
import com.hyperion.planning.sql.keydef.*;
import com.hyperion.planning.sql.actions.*;
import com.hyperion.planning.sql.actions.ActionMethod;

import java.util.*;

import java.sql.Connection;
import java.sql.SQLException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.concurrent.Callable;

import com.hyperion.planning.metadata.HspMDMember;
import com.hyperion.planning.metadata.MDMembers.MDMember;
import com.hyperion.planning.metadata.MDUtils;

import com.hyperion.planning.migration.HspUpgradeHelper;
import com.hyperion.planning.modules.common.HspModuleRegistry;

import org.apache.commons.lang.ArrayUtils;

import com.hyperion.planning.odl.HspODLLogger;
import com.hyperion.planning.odl.HspODLMsgs;
import com.hyperion.planning.olap.HspEssApplication;
import com.hyperion.planning.olap.HspEssCube;
import com.hyperion.planning.olap.HspEssbaseObjectHelper;
import com.hyperion.planning.olap.HspMemberHelper;

import com.hyperion.planning.olap.HspMemberOnFlyBucketCacheLoader;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.Validate;

import com.hyperion.planning.utils.HspSimpleCurrencyUtils;

import com.hyperion.planning.olap.HspRestObjectHelper;
import com.hyperion.planning.utils.HspMembersByAttributesFilterer;
import com.hyperion.planning.utils.HspMemberToStringsConverter;
import com.hyperion.planning.utils.HspTransformerUtils;
import com.hyperion.planning.validcombo.HspVCRule;
import com.hyperion.planning.validcombo.HspVCSubRule;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import oracle.epm.api.model.ApplicationType;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.PredicateUtils;


public class HspDEDBImpl implements HspDEDB {
    private final static HspODLLogger logger = HspODLLogger.getLogger();

    public HspDEDBImpl(HspStateMgr hspStateMgr, HspSQL hspSQL, HspOLAP hspOLAP) {

        this.hspStateMgr = hspStateMgr;
        this.hspSQL = hspSQL;
        this.hspOLAP = hspOLAP;
    }

    public synchronized void initializeDB(HspJS hspJS, int sessionId) {
        logger.entering(hspJS, sessionId);

        this.hspJS = hspJS;
        this.hspSecDB = hspJS.getSecDB(sessionId);
        this.hspCurDB = hspJS.getCurDB(sessionId);
        this.hspPMDB = hspJS.getPMDB(sessionId);
        this.hspAlsDB = hspJS.getAlsDB(sessionId);
        this.hspCalDB = hspJS.getCalDB(sessionId);
        this.hspFMDB = hspJS.getFMDB(sessionId);
        this.hspPrefDB = hspJS.getPrefDB(sessionId);
        this.hspSystemConfig = hspJS.getSystemCfg();
        this.hspCubeLinkDB = hspJS.getCubeLinkDB(sessionId);
        this.internalSessionId = sessionId;

        hspRepDimMbrCacheMgr = new HspReportingDimMemberCacheManagerImpl(hspOLAP, hspJS, sessionId);

        resetBufferResourceDetails();
        logBufferResourceDetails();
        initCache();
        if (hspJS != null) {
            notifier = hspJS.getChangeEventNotifier(sessionId);
            if (notifier != null) {
                // Since PM dimensions do not get flushed in changeEventOccured like all other dimensions, it is important
                // for this change event listener to be the first in this method -- Ujwala
                // If this is moved below, any newly added and deleted PM dimensions may not get added and flushed from membersCache
                notifier.addChangeEventListener(new HspUCChangeEventHandler(pmDimensionCache), HspPMDimension.class);
                notifier.addChangeEventListener(new HspUCChangeEventHandler(replacementDimensionCache), HspReplacementDimension.class);

                // Special eventsed
                notifier.addChangeEventListener(this, HspPMDimension.class);

                notifier.addChangeEventListener(this, HspDimension.class); //Listens to Dimension changes.
                notifier.addChangeEventListener(this, HspAttributeDimension.class); //Listens to Attribute Dimension changes.
                notifier.addChangeEventListener(this, HspMember.class); //Listens to Member changes incase they are actually dimension changes.
                notifier.addChangeEventListener(this, HspObject.class); //Listens to Object changes.

                notifier.addChangeEventListener(new HspPostMemberSaveCallbackEventHandler(hspJS), HspMemberInfo.class);

                //Standard UC Member events
                notifier.addChangeEventListener(new HspUCChangeEventHandler(entityCache, HspEntity.class), HspEntity.class);
                notifier.addChangeEventListener(new HspUCChangeEventHandler(accountCache, HspAccount.class), HspAccount.class);
                notifier.addChangeEventListener(new HspUCChangeEventHandler(scenarioCache, HspScenario.class), HspScenario.class);
                notifier.addChangeEventListener(new HspUCChangeEventHandler(versionCache, HspVersion.class), HspVersion.class);
                notifier.addChangeEventListener(new HspUCChangeEventHandler(timePeriodCache, HspTimePeriod.class), HspTimePeriod.class);
                notifier.addChangeEventListener(new HspUCChangeEventHandler(yearCache, HspYear.class), HspYear.class);
                notifier.addChangeEventListener(new HspUCChangeEventHandler(metricCache, HspMetric.class), HspMetric.class);

                notifier.addChangeEventListener(new HspUCChangeEventHandler(dpDimensionCache), HspDPDimension.class);
                notifier.addChangeEventListener(new HspMPUCChangeEventHandler(objectNotesCache, objectNoteMPCKeyGenerator), HspObjectNote.class);
                notifier.addChangeEventListener(new HspUCChangeEventHandler(brScenarioVersionCache), HspDPSVBRBinding.class);
                notifier.addChangeEventListener(new HspUCChangeEventHandler(mbrOnFlyDetailCache), HspMemberOnFlyDetail.class);
                notifier.addChangeEventListener(new HspGCChangeEventHandler(mbrOnFlyBucketCache), HspMemberOnFlyBucket.class);
                notifier.addChangeEventListener(new HspUCChangeEventHandler(mbrPTPropsCache), HspMemberPTProps.class);

                //Currency cache must be a GC Handler, since currencies that may not
                //exist in this cache might be fired as events.  For example,
                //predefined currencies.
                notifier.addChangeEventListener(new HspGCChangeEventHandler(currencyCache), HspCurrency.class);
                //notifier.addChangeEventListener(new HspGCChangeEventHandler(labelDynamicMembersCache), HspMember.class);
                notifier.addChangeEventListener(new HspUCChangeEventHandler(cubeCache, HspCube.class), HspCube.class);
                notifier.addChangeEventListener(new HspUCChangeEventHandler(cubeSLMappingCache, HspCubeSLMapping.class), HspCubeSLMapping.class);
                notifier.addChangeEventListener(new HspUCChangeEventHandler(essbaseServerCache, HspExternalServer.class), HspExternalServer.class);
                //notifier.addChangeEventListener(new HspUCChangeEventHandler(essbaseCubeCache, HspEssbaseCube.class), HspEssbaseCube.class);
                notifier.addChangeEventListener(new HspUCChangeEventHandler(userVariableCache, HspUserVariable.class), HspUserVariable.class);
                notifier.addChangeEventListener(new HspUCChangeEventHandler(userVariableValueCache, HspUserVariableValue.class), HspUserVariableValue.class);

                // The derivedEnumCache needs to listen to all objects that
                // have derived enumerations.  This method calls calls
                // addChangeEventListener for each such pairing.
                Collection<Class> derivedFromClasses = generatedEnumCacheLoader.getDerivedFromClasses();
                if (derivedFromClasses != null && derivedFromClasses.size() > 0) {
                    HspGCChangeEventHandler derivedEnumEventHandler = new HspGCChangeEventHandler(generatedEnumCache);
                    for (Class c : derivedFromClasses)
                        notifier.addChangeEventListener(derivedEnumEventHandler, c);
                }

                // The HspVirtualEnumEventHandler uses the addGlobalChangeEventListener because it must be called after all other specific listeners, specifically,
                // all member listeners including the ones that were reordered by calling the recreateCustomMemberHandlers(). All uses of addGlobalChangeEventListener
                // need approval from an architect. The usage by HspVirtualEnumEventHandler was approved by sbakey on 29 July 2015.
                notifier.addGlobalChangeEventListener(new HspVirtualEnumEventHandler(enumCache, dimMembersImpactedTimeMap, new HspSimpleVirtualEnumEntryGenerator(hspJS, hspAlsDB, sessionId)));

                // TODO: This is a temperary fix for FCCS, need to discuss this with Shaun when all of us are back from Dec break (23rd Dec 2015).
                //HspUser appOwner = hspSecDB.getApplicationOwner();
                //int appOwnerSessionId = hspStateMgr.createImpersonationSession(appOwner.getId(), sessionId);
                //notifier.addGlobalChangeEventListener(new HspPostMemberSaveCallbackEventHandler(hspJS, appOwnerSessionId));
                //notifier.addChangeEventListener(new HspPostMemberSaveCallbackEventHandler(hspJS, appOwnerSessionId), HspMember.class);

                notifier.addChangeEventListener(new HspUCChangeEventHandler(udaCache), HspUDA.class);
                notifier.addChangeEventListener(new HspUCChangeEventHandler(udaBindingCache), HspUDABinding.class);
                notifier.addChangeEventListener(new HspGCChangeEventHandler(driverMemberCache), HspDriverMember.class);
                notifier.addChangeEventListener(new HspUCChangeEventHandler(lineItemMemberCache), HspLineItemMember.class);
                // Flush the currencyEnumsCache when currencies are changed.

                notifier.addChangeEventListener(new HspGCChangeEventHandler(generatedCurrencyEnumsCache), HspCurrency.class);
                notifier.addChangeEventListener(new HspGCChangeEventHandler(generatedSmartListsEnumsCache), HspEnumeration.class);

                notifier.addChangeEventListener(new HspGCChangeEventHandler(generatedFXTablesEnumsCache), HspFXTableInfo.class);
                notifier.addChangeEventListener(new HspGCChangeEventHandler(generatedYearsEnumsCache), HspYear.class);
                notifier.addChangeEventListener(new HspGCChangeEventHandler(generatedSourcePlanTypeEnumsCache), HspCube.class);
                notifier.addChangeEventListener(new HspGCChangeEventHandler(generatedPeriodsEnumsCache), HspTimePeriod.class);
                notifier.addChangeEventListener(new HspGCChangeEventHandler(generatedAtttribDimMbrsEnumCache), HspAttributeMember.class);
                notifier.addChangeEventListener(new HspUCChangeEventHandler(cubeMappingCache, HspDPCubeMapping.class), HspDPCubeMapping.class);

                recreateCustomMemberHandlers();

                moduleDimensionAdapter = getModuleDimensionAdapter(sessionId);
            }

            // Instantiate this class after the caches and change event
            // handlers have been initialized
            hspDimensionCustomizationMgr = new HspDimensionCustomizationMgrImpl(hspJS, sessionId);

        }
        logger.exiting();
    }

    /**
     * This method deletes the old custom dimension handlers and adds new custom
     * dimensions handlers that handle change prop events for custom dimensions.
     * This must be done when ever a custom dimensions is added or deleted, so that
     * it can either be added to or deleted from the change event notifier.
     */
    protected synchronized void recreateCustomMemberHandlers() {
        if (notifier != null) {
            //Removed the old Handlers
            if (customDimHandlers == null)
                customDimHandlers = new Vector<HspUCCustomMemberChangeEventHandler>();
            else {
                for (int loop1 = 0; loop1 < customDimHandlers.size(); loop1++) {
                    HspChangeEventListener listener = customDimHandlers.elementAt(loop1);
                    if (listener != null)
                        notifier.removeChangeEventListener(listener, HspMember.class);
                }
            }
            //Now, remove all old handlers from the handler list.
            customDimHandlers.removeAllElements();
            //Add the new custom dimensions handlers.
            Vector customDimensions = getCustomDimensionsInternal(HspConstants.PLAN_TYPE_ALL);
            if (customDimensions != null) {
                for (int loop1 = 0; loop1 < customDimensions.size(); loop1++) {
                    HspDimension custDim = (HspDimension)customDimensions.elementAt(loop1);
                    if (custDim != null) {
                        HspUpdateableCache<HspMember> cache = (HspUpdateableCache<HspMember>)getMembersCache(custDim.getDimId());
                        if (cache != null) {
                            //System.out.println("change event listener for cust dim: " + custDim.getMemberName() + " ID: " + custDim.getId());
                            HspUCCustomMemberChangeEventHandler handler = new HspUCCustomMemberChangeEventHandler(cache, custDim.getId(), HspMember.class);
                            customDimHandlers.add(handler);
                            notifier.addChangeEventListener(handler, HspMember.class);
                        }
                    }
                }
            }
            //TODO: Why is this not removing the existing listeners?
            //Add the new attribute dimensions handlers.
            customDimensions = getAttributeDimensionsInternal();
            if (customDimensions != null) {
                for (int loop1 = 0; loop1 < customDimensions.size(); loop1++) {
                    HspDimension custDim = (HspDimension)customDimensions.elementAt(loop1);
                    if (custDim != null) {
                        //System.out.println("change event listener for dim: " + custDim.getMemberName() + " ID: " + custDim.getId());
                        HspUpdateableCache<HspMember> cache = (HspUpdateableCache<HspMember>)getMembersCache(custDim.getDimId());
                        if (cache != null) {
                            HspUCCustomMemberChangeEventHandler handler = new HspUCCustomMemberChangeEventHandler(cache, custDim.getId(), HspAttributeMember.class);
                            //customDimHandlers.add(handler);
                            notifier.addChangeEventListener(handler, HspAttributeMember.class);
                        }
                    }
                }
            }

            //Removed the old Handlers
            if (replacementDimHandlers == null)
                replacementDimHandlers = new Vector<HspUCCustomMemberChangeEventHandler>();
            else {
                for (HspChangeEventListener listener : replacementDimHandlers) {
                    if (listener != null)
                        notifier.removeChangeEventListener(listener, HspMember.class);
                }
            }
            // Handle replacement dimensions
            replacementDimHandlers.removeAllElements();
            List<HspReplacementDimension> replacementDimensions = replacementDimensionCache.getUnfilteredCache();
            if (replacementDimensions != null) {
                for (HspReplacementDimension replacementDimension : replacementDimensions) {
                    if (replacementDimension != null) {
                        HspUpdateableCache<HspMember> cache = (HspUpdateableCache<HspMember>)getMembersCache(replacementDimension.getDimId());
                        if (cache != null) {
                            HspUCCustomMemberChangeEventHandler handler = new HspUCCustomMemberChangeEventHandler(cache, replacementDimension.getId(), HspReplacementMember.class);
                            replacementDimHandlers.add(handler);
                            notifier.addChangeEventListener(handler, HspReplacementMember.class);
                        }
                    }
                }
            }

            // Handle PM Member
            if (pmDimHandlers == null)
                pmDimHandlers = new Vector<HspGCChangeEventHandler>();
            else {
                for (HspChangeEventListener listener : pmDimHandlers) {
                    if (listener != null) {
                        //todo: properly handle other events such as member name changes and member hierarchy changes
                        notifier.removeChangeEventListener(listener, HspPMMember.class);
                    }
                }
            }
            //Now, remove all old handlers from the handler list.
            pmDimHandlers.removeAllElements();
            //Add the new pm members handlers.
            Vector<HspPMDimension> dimensions = pmDimensionCache.getUnfilteredCache();
            if (dimensions != null) {
                for (HspPMDimension dimension : dimensions) {
                    if (dimension != null) {
                        GenericCache<HspMember> cache = getMembersCache(dimension.getDimId());
                        if (cache != null) {
                            // If a primary or secondary member is changed, flush this cache, as the reload
                            // will simply rebuild the tree from the primary and secondary caches.
                            //HspGCChangeEventHandler handler = new HspGCChangeEventHandler(cache);
                            HspGCChangeEventHandler handler = new HspGCCustomPMMemberChangeEventHandler(cache, dimension.getDimId());
                            pmDimHandlers.add(handler);
                            notifier.addChangeEventListener(handler, HspPMMember.class);
                        }
                    }
                }
            }

            //Removed the old Handlers
            if (dpDimHandlers == null)
                dpDimHandlers = new Vector<HspUCChangeEventHandler>();
            else {
                for (int loop1 = 0; loop1 < dpDimHandlers.size(); loop1++) {
                    HspChangeEventListener listener = dpDimHandlers.elementAt(loop1);
                    if (listener != null)
                        notifier.removeChangeEventListener(listener, HspDPMember.class);
                }
            }
            //Now, remove all old handlers from the handler list.
            dpDimHandlers.removeAllElements();
            //Add the new DP dimensions handlers.
            Vector<HspDPDimension> dpDimensions = dpDimensionCache.getUnfilteredCache();
            if (dpDimensions != null) {
                for (HspDPDimension dpDimension : dpDimensions) {
                    if (dpDimension != null) {
                        HspUpdateableCache<HspMember> cache = (HspUpdateableCache<HspMember>)getMembersCache(dpDimension.getDimId());
                        if (cache != null) {
                            HspUCCustomMemberChangeEventHandler handler = new HspUCCustomMemberChangeEventHandler(cache, dpDimension.getId(), HspDPMember.class);
                            //HspUCChangeEventHandler handler = new HspUCChangeEventHandler(cache, HspDPMember.class);
                            dpDimHandlers.add(handler);
                            notifier.addChangeEventListener(handler, HspDPMember.class);
                        }
                    }
                }
            }

            //            if(hspJS.getSystemCfg().isSimpleMultiCurrency()){
            //                HspUpdateableCache<HspMember> simCurDimCache =
            //                    (HspUpdateableCache<HspMember>)getMembersCache(HspConstants.kDimensionSimpleCurrency);
            //                if (simCurDimCache != null)
            //                    notifier.addChangeEventListener(new HspUCCustomMemberChangeEventHandler(simCurDimCache, HspConstants.kDimensionSimpleCurrency, HspMember.class),
            //                                                    HspMember.class);
            //            }
            // Add handler for Budget Request, Decision Packages, Budget Requests dimension since we do not maintain a seperate cache for this dimension's
            // members and it is not a custom dimension.
            try {
                HspUpdateableCache<HspMember> brDimCache = (HspUpdateableCache<HspMember>)getMembersCache(HspConstants.kDimensionBudgetRequest);
                if (brDimCache != null)
                    notifier.addChangeEventListener(new HspUCCustomMemberChangeEventHandler(brDimCache, HspConstants.kDimensionBudgetRequest, HspMember.class), HspMember.class);
                HspUpdateableCache<HspMember> dpASODimCache = (HspUpdateableCache<HspMember>)getMembersCache(HspConstants.kDimensionDecisionPackagesASO);
                if (dpASODimCache != null)
                    notifier.addChangeEventListener(new HspUCCustomMemberChangeEventHandler(dpASODimCache, HspConstants.kDimensionDecisionPackagesASO, HspMember.class), HspMember.class);
                HspUpdateableCache<HspMember> brASODimCache = (HspUpdateableCache<HspMember>)getMembersCache(HspConstants.kDimensionBudgetRequestsASO);
                if (brASODimCache != null)
                    notifier.addChangeEventListener(new HspUCCustomMemberChangeEventHandler(brASODimCache, HspConstants.kDimensionBudgetRequestsASO, HspMember.class), HspMember.class);
            } catch (Exception e) {
                logger.finer("For non-DP app, Request dimension not needed");
            }
        }
    }

    protected <MBR extends HspMember> GenericCache<MBR> getMembersCache(int dimId) {
        synchronized (memberCacheHash) {
            //noinspection unchecked
            GenericCache<MBR> memberCache = (GenericCache<MBR>)memberCacheHash.get(dimId);

            if (memberCache == null)
                memberCache = (GenericCache<MBR>)hspRepDimMbrCacheMgr.getReportingCubeMemberCache(dimId);
            if (memberCache == null)
                throw new InvalidDimensionException(Integer.toString(dimId));

            return memberCache;
        }
    }

    private synchronized void initCache() {
        logger.entering();
        //dimensionCache must be done first as all ther caches use
        //this cache for getting dimension root members.
        dimensionCache = new HspUpdateableCache<HspDimension>(new JDBCCacheLoader<HspDimension>(HspDimension.class, "SQL_GET_DIMENSIONS", hspSQL), hspSecDB, new CachedObjectKeyDef[] { orderedObjectIdKeyDef, cachedObjectDefaultKeyDef, memberPsIdKeyDef });
        //attributeDimensionCache = new HspUpdateableCache<HspAttributeDimension>(new JDBCCacheLoader<HspAttributeDimension>(HspAttributeDimension.class, "SQL_GET_ATTRIBUTE_DIMENSIONS", hspSQL), hspSecDB, new CachedObjectKeyDef[] { orderedObjectIdKeyDef, cachedObjectDefaultKeyDef, memberPsIdKeyDef });

        // secclassId is used to relate the memberSelections to its parent HspMbrSelection (selId), after the parent child
        // relationship is created by the RelationshipCacheLoader, set the secclassId back to -1;
        String transformExpr = "memberSelections == null ? null : memberSelections.{secclassId = -1}";
        String[] mbrSelectionParams = { String.valueOf(HspMbrSelection.TYPE_REF_ATTRDIM_FILTER) };
        // TODO: Member selection items should be filtered by selectio
        RelationshipCacheLoader<HspMbrSelection> attrDimMbrSelCacheLoader =
            new RelationshipCacheLoader<HspMbrSelection>(new JDBCCacheLoader<HspMbrSelection>(HspMbrSelection.class, "SQL_GET_MBR_SELS_BY_TYPE", new String[] { Integer.toString(HspMbrSelection.TYPE_REF_ATTRDIM_FILTER) }, hspSQL));
        attrDimMbrSelCacheLoader.addRelationship("memberSelections", CachedObjectIdKeyDef.CACHED_OBJECT_ID_KEY, new JDBCCacheLoader<HspFormMember>(HspFormMember.class, "SQL_GET_MBR_SEL_ITEMS", hspSQL), FormMemberBySelIdKeyDef.FORM_MEMBER_SELECTION_BY_SEL_ID_KEY);
        OgnlTransformCacheLoader<HspMbrSelection> transformParentIdCacheLoader = new OgnlTransformCacheLoader<HspMbrSelection>(attrDimMbrSelCacheLoader, HspMbrSelection.class, transformExpr);

        RelationshipCacheLoader<HspAttributeDimension> attributeDimCacheLoader = new RelationshipCacheLoader<HspAttributeDimension>(new JDBCCacheLoader<HspAttributeDimension>(HspAttributeDimension.class, "SQL_GET_ATTRIBUTE_DIMENSIONS", hspSQL));
        attributeDimCacheLoader.addRelationship("mbrSelection", AttrDimMemberSelIdKeyDef.ATTR_DIM_MBR_SEL_ID_KEY, transformParentIdCacheLoader, CachedObjectIdKeyDef.CACHED_OBJECT_ID_KEY);
        attributeDimensionCache = new HspUpdateableCache<HspAttributeDimension>(attributeDimCacheLoader, hspSecDB, new CachedObjectKeyDef[] { orderedObjectIdKeyDef, cachedObjectDefaultKeyDef });


        hiddenDimensionsCache = new HspUpdateableCache<HspDimension>(new JDBCCacheLoader<HspDimension>(HspDimension.class, "SQL_GET_HIDDEN_DIMENSIONS", hspSQL), hspSecDB);
        metricDimensionCache = new HspUpdateableCache<HspDimension>(new JDBCCacheLoader<HspDimension>(HspDimension.class, "SQL_GET_METRIC_DIMENSIONS", hspSQL), hspSecDB);

        List<HspDimension> virtualDimensions = new ArrayList<HspDimension>(1);
        virtualDimensions.add(MDMember.MD_DIMENSION);
        virtualDimensionsCache = new HspUpdateableCache<HspDimension>(new CollectionCacheLoader<HspDimension>(virtualDimensions, HspDimension.class), hspSecDB, new CachedObjectKeyDef[] { orderedObjectIdKeyDef, cachedObjectDefaultKeyDef, memberPsIdKeyDef });

        //dimMembersCache = new MultiPartCache(HspMember.class, "SQL_GET_CUST_DIM_MEMBERS", hspSQL, hspSecDB);
        //dimMembersCache.setRootAsKey(0);	//We use element 0 in the params array we send as the root key
        cubeSLMappingCache = new HspUpdateableCache<HspCubeSLMapping>(new JDBCCacheLoader<HspCubeSLMapping>(HspCubeSLMapping.class, "SQL_GET_CUBE_SL_MAPPINGS", hspSQL), hspSecDB);
        cubeMappingCache = new HspUpdateableCache<HspDPCubeMapping>(new JDBCCacheLoader<HspDPCubeMapping>(HspDPCubeMapping.class, "SQL_GET_DP_CUBE_MAPPINGS", hspSQL), hspSecDB, new CachedObjectKeyDef[] { dpCubeMappingFromToKeyDef });
        cubeCache = new HspUpdateableCache<HspCube>(new JDBCCacheLoader<HspCube>(HspCube.class, "SQL_GET_CUBES", hspSQL), hspSecDB);
        initEssbaseServerCache();
        //essbaseCubeCache = new HspUpdateableCache<HspEssbaseCube>(new JDBCCacheLoader<HspEssbaseCube>(HspEssbaseCube.class, "SQL_GET_ESSBASE_CUBES", hspSQL), hspSecDB);

        // Relationship cache loader to load member formula propeties for a given rule.
        //RelationshipCacheLoader<HspAccount> accountCacheLoader = new RelationshipCacheLoader<HspAccount>(new JDBCCacheLoader<HspAccount>(HspAccount.class, "SQL_GET_ACCOUNTS", hspSQL));
        //accountCacheLoader.addRelationship("memberPTProps", orderedObjectIdKeyDef, new JDBCCacheLoader<HspMemberPTProps>(HspMemberPTProps.class, "SQL_GET_MEMBER_FORMULA_PROPS", hspSQL), mbrPTPropsMemberIdKeyDef);
        //accountCache = new HspUpdateableCache<HspAccount>(accountCacheLoader, hspSecDB, new CachedObjectKeyDef[] { orderedObjectIdKeyDef, cachedObjectDefaultKeyDef, memberBaseMemberIdKeyDef, memberPsIdKeyDef, accountSubAccountKeyDef, hspObjectOldNameKeyDef });
        accountCache =
                new HspUpdateableCache<HspAccount>(new JDBCCacheLoader<HspAccount>(HspAccount.class, "SQL_GET_ACCOUNTS", hspSQL), hspSecDB, new CachedObjectKeyDef[] { orderedObjectIdKeyDef, cachedObjectDefaultKeyDef, memberBaseMemberIdKeyDef, memberPsIdKeyDef, accountSubAccountKeyDef,
                                                                                                                                                                       hspObjectOldNameKeyDef });
        entityCache =
                new HspUpdateableCache<HspEntity>(new JDBCCacheLoader<HspEntity>(HspEntity.class, "SQL_GET_ENTITIES", hspSQL), hspSecDB, new CachedObjectKeyDef[] { orderedObjectIdKeyDef, cachedObjectDefaultKeyDef, memberBaseMemberIdKeyDef, memberPsIdKeyDef, entityRequisitionNumberKeyDef,
                                                                                                                                                                    hspObjectOldNameKeyDef });
        scenarioCache =
                new HspUpdateableCache<HspScenario>(new JDBCCacheLoader<HspScenario>(HspScenario.class, "SQL_GET_SCENARIOS", hspSQL), hspSecDB, new CachedObjectKeyDef[] { orderedObjectIdKeyDef, cachedObjectDefaultKeyDef, memberBaseMemberIdKeyDef, memberPsIdKeyDef, hspObjectOldNameKeyDef });

        // Relationship cache loader to load member formula propeties for a given rule.
        //RelationshipCacheLoader<HspVersion> versionCacheLoader = new RelationshipCacheLoader<HspVersion>(new JDBCCacheLoader<HspVersion>(HspVersion.class, "SQL_GET_VERSIONS", hspSQL));
        //versionCacheLoader.addRelationship("memberPTProps", orderedObjectIdKeyDef, new JDBCCacheLoader<HspMemberPTProps>(HspMemberPTProps.class, "SQL_GET_MEMBER_FORMULA_PROPS", hspSQL), mbrPTPropsMemberIdKeyDef);
        //versionCache = new HspUpdateableCache<HspVersion>(versionCacheLoader, hspSecDB, new CachedObjectKeyDef[] { orderedObjectIdKeyDef, cachedObjectDefaultKeyDef, memberBaseMemberIdKeyDef, memberPsIdKeyDef, hspObjectOldNameKeyDef });
        versionCache =
                new HspUpdateableCache<HspVersion>(new JDBCCacheLoader<HspVersion>(HspVersion.class, "SQL_GET_VERSIONS", hspSQL), hspSecDB, new CachedObjectKeyDef[] { orderedObjectIdKeyDef, cachedObjectDefaultKeyDef, memberBaseMemberIdKeyDef, memberPsIdKeyDef, hspObjectOldNameKeyDef });

        //        RelationshipCacheLoader<HspAccount> accountCacheLoader = new RelationshipCacheLoader<HspAccount>(new JDBCCacheLoader<HspAccount>(HspAccount.class, "SQL_GET_ACCOUNTS", hspSQL));
        //        accountCacheLoader.addRelationship("memberOnFlyDetail", orderedObjectIdKeyDef, new JDBCCacheLoader<HspMemberOnFlyDetail>(HspMemberOnFlyDetail.class, "SQL_GET_MEMBER_ON_FLY_DETAIL", hspSQL), mbrOnFlyParentIdKeyDef);
        //        accountCache = new HspUpdateableCache<HspAccount>(accountCacheLoader, hspSecDB, new CachedObjectKeyDef[] { orderedObjectIdKeyDef, cachedObjectDefaultKeyDef, memberBaseMemberIdKeyDef, memberPsIdKeyDef, accountSubAccountKeyDef, mbrIsDynamicChildEnabledKeyDef, hspObjectOldNameKeyDef });
        //
        //        RelationshipCacheLoader<HspEntity> entityCacheLoader = new RelationshipCacheLoader<HspEntity>(new JDBCCacheLoader<HspEntity>(HspEntity.class, "SQL_GET_ENTITIES", hspSQL));
        //        entityCacheLoader.addRelationship("memberOnFlyDetail", orderedObjectIdKeyDef, new JDBCCacheLoader<HspMemberOnFlyDetail>(HspMemberOnFlyDetail.class, "SQL_GET_MEMBER_ON_FLY_DETAIL", hspSQL), mbrOnFlyParentIdKeyDef);
        //        entityCache = new HspUpdateableCache<HspEntity>(entityCacheLoader, hspSecDB, new CachedObjectKeyDef[] { orderedObjectIdKeyDef, cachedObjectDefaultKeyDef, memberBaseMemberIdKeyDef, memberPsIdKeyDef, entityRequisitionNumberKeyDef, mbrIsDynamicChildEnabledKeyDef, hspObjectOldNameKeyDef });
        //
        //        RelationshipCacheLoader<HspScenario> scenarioCacheLoader = new RelationshipCacheLoader<HspScenario>(new JDBCCacheLoader<HspScenario>(HspScenario.class, "SQL_GET_SCENARIOS", hspSQL));
        //        scenarioCacheLoader.addRelationship("memberOnFlyDetail", orderedObjectIdKeyDef, new JDBCCacheLoader<HspMemberOnFlyDetail>(HspMemberOnFlyDetail.class, "SQL_GET_MEMBER_ON_FLY_DETAIL", hspSQL), mbrOnFlyParentIdKeyDef);
        //        scenarioCache = new HspUpdateableCache<HspScenario>(scenarioCacheLoader, hspSecDB, new CachedObjectKeyDef[] { orderedObjectIdKeyDef, cachedObjectDefaultKeyDef, memberBaseMemberIdKeyDef, memberPsIdKeyDef, mbrIsDynamicChildEnabledKeyDef, hspObjectOldNameKeyDef });
        //
        //        RelationshipCacheLoader<HspVersion> versionCacheLoader = new RelationshipCacheLoader<HspVersion>(new JDBCCacheLoader<HspVersion>(HspVersion.class, "SQL_GET_VERSIONS", hspSQL));
        //        versionCacheLoader.addRelationship("memberOnFlyDetail", orderedObjectIdKeyDef, new JDBCCacheLoader<HspMemberOnFlyDetail>(HspMemberOnFlyDetail.class, "SQL_GET_MEMBER_ON_FLY_DETAIL", hspSQL), mbrOnFlyParentIdKeyDef);
        //        versionCache = new HspUpdateableCache<HspVersion>(versionCacheLoader, hspSecDB, new CachedObjectKeyDef[] { orderedObjectIdKeyDef, cachedObjectDefaultKeyDef, memberBaseMemberIdKeyDef, memberPsIdKeyDef, mbrIsDynamicChildEnabledKeyDef, hspObjectOldNameKeyDef });

        timePeriodCache =
                new HspUpdateableCache<HspTimePeriod>(new JDBCCacheLoader<HspTimePeriod>(HspTimePeriod.class, "SQL_GET_TIME_PERIODS", hspSQL), hspSecDB, new CachedObjectKeyDef[] { orderedObjectIdKeyDef, cachedObjectDefaultKeyDef, memberBaseMemberIdKeyDef, memberPsIdKeyDef,
                                                                                                                                                                                    hspObjectOldNameKeyDef });
        yearCache = new HspUpdateableCache<HspYear>(new JDBCCacheLoader<HspYear>(HspYear.class, "SQL_GET_YEARS", hspSQL), hspSecDB, new CachedObjectKeyDef[] { orderedObjectIdKeyDef, cachedObjectDefaultKeyDef, memberBaseMemberIdKeyDef, memberPsIdKeyDef, hspObjectOldNameKeyDef });
        currencyCache =
                new HspUpdateableCache<HspCurrency>(new JDBCCacheLoader<HspCurrency>(HspCurrency.class, "SQL_GET_CURRENCIES_ALL", hspSQL), hspSecDB, new CachedObjectKeyDef[] { orderedObjectIdKeyDef, cachedObjectDefaultKeyDef, memberBaseMemberIdKeyDef, memberPsIdKeyDef, hspObjectOldNameKeyDef });
        metricCache = new HspUpdateableCache<HspMetric>(new JDBCCacheLoader<HspMetric>(HspMetric.class, "SQL_GET_METRICS", hspSQL), hspSecDB, new CachedObjectKeyDef[] { orderedObjectIdKeyDef, cachedObjectDefaultKeyDef, memberBaseMemberIdKeyDef });
        replacementDimensionCache = new HspUpdateableCache<HspReplacementDimension>(new JDBCCacheLoader<HspReplacementDimension>(HspReplacementDimension.class, "SQL_GET_REPLACEMENT_DIMENSIONS", hspSQL), hspSecDB);
        pmDimensionCache = new HspUpdateableCache<HspPMDimension>(new JDBCCacheLoader<HspPMDimension>(HspPMDimension.class, "SQL_GET_PM_DIMENSIONS", hspSQL), hspSecDB); //, new CachedObjectKeyDef[] {orderedObjectIdKeyDef, cachedObjectDefaultKeyDef, memberPsIdKeyDef});
        dpDimensionCache = new HspUpdateableCache<HspDPDimension>(new JDBCCacheLoader<HspDPDimension>(HspDPDimension.class, "SQL_GET_DP_DIMENSIONS", hspSQL), hspSecDB, new CachedObjectKeyDef[] { orderedObjectIdKeyDef, cachedObjectDefaultKeyDef });

        //brDimensionCache = new HspUpdateableCache<HspDimension>(new JDBCCacheLoader<HspDimension>(HspDimension.class, "SQL_GET_BR_DIMENSIONS", hspSQL), hspSecDB);
        brScenarioVersionCache = new HspUpdateableCache<HspDPSVBRBinding>(new JDBCCacheLoader<HspDPSVBRBinding>(HspDPSVBRBinding.class, "SQL_GET_DP_SVBR_BINDINGS", hspSQL), hspSecDB, new CachedObjectKeyDef[] { brScenarioIdVersionIdKeyDef });
        mbrOnFlyDetailCache =
                new HspUpdateableCache<HspMemberOnFlyDetail>(new JDBCCacheLoader<HspMemberOnFlyDetail>(HspMemberOnFlyDetail.class, "SQL_GET_MEMBER_ON_FLY_DETAIL", hspSQL), hspSecDB, new CachedObjectKeyDef[] { /*mbrOnFlyParentIdCurrentBucketKeyDef, */mbrOnFlyParentIdKeyDef });
        mbrOnFlyBucketCache = new GenericCache<HspMemberOnFlyBucket>(new HspMemberOnFlyBucketCacheLoader(hspJS, internalSessionId), null, new CachedObjectKeyDef[] { mofBucketParentIdBucketIndexKeyDef });
        mbrPTPropsCache = new HspUpdateableCache<HspMemberPTProps>(new JDBCCacheLoader<HspMemberPTProps>(HspMemberPTProps.class, "SQL_GET_MEMBER_FORMULA_PROPS", hspSQL), hspSecDB, new CachedObjectKeyDef[] { mbrPTPropsMemberIdKeyDef, mbrPTPropsMemberIdPlanTypeKeyDef });
        objectNotesCache = new MultiPartCache<HspObjectNote>(new JDBCCacheLoader<HspObjectNote>(HspObjectNote.class, "SQL_GET_OBJECT_NOTES", hspSQL), hspSecDB, 10000, new CachedObjectKeyDef[] { dpNoteAttachmentIdKeyDef, dpNoteAttachmentKeyDef, objNoteTypeKeyDef });
        //labelDynamicMembersCache = new HspUpdateableCache(new JDBCCacheLoader(HspCurrency.class,"SQL_GET_LABLE_DYNAMIC_MEMBERS", hspSQL), hspSecDB, new CachedObjectKeyDef[] {orderedObjectIdKeyDef, cachedObjectDefaultKeyDef, memberPsIdKeyDef});
        fxTablesInfoCache = new HspUpdateableCache<HspCurrency>(new JDBCCacheLoader<HspCurrency>(HspCurrency.class, "SQL_GET_FX_TABLES", hspSQL), hspSecDB);

        driverMemberCache = new GenericCache<HspDriverMember>(new JDBCCacheLoader<HspDriverMember>(HspDriverMember.class, "SQL_GET_DRIVER_MEMBERS", hspSQL), hspSecDB, new CachedObjectKeyDef[] { driverMemberBaseDimIdKeyDef });

        //Line Item member cache for incremental daat load
        // String transformExpr = "memberSelections == null ? null : memberSelections.{secclassId = -1}";
        RelationshipCacheLoader<HspMbrSelection> itemItemMemberSelectionCacheLoader =
            new RelationshipCacheLoader<HspMbrSelection>(new JDBCCacheLoader<HspMbrSelection>(HspMbrSelection.class, "SQL_GET_MBR_SELS_BY_TYPE", new String[] { Integer.toString(HspMbrSelection.TYPE_DL_LINE_ITEM_MBR) }, hspSQL));
        itemItemMemberSelectionCacheLoader.addRelationship("memberSelections", dimSelIdKeyDef, new JDBCCacheLoader<HspFormMember>(HspFormMember.class, "SQL_GET_MBR_SEL_ITEMS", hspSQL), FormMemberBySelIdKeyDef.FORM_MEMBER_SELECTION_BY_SEL_ID_KEY);
        OgnlTransformCacheLoader<HspMbrSelection> transformParentIdCacheLoaderLI = new OgnlTransformCacheLoader<HspMbrSelection>(itemItemMemberSelectionCacheLoader, HspMbrSelection.class, transformExpr);
        RelationshipCacheLoader<HspLineItemMember> lineItemMemberCacheLoader = new RelationshipCacheLoader<HspLineItemMember>(new JDBCCacheLoader<HspLineItemMember>(HspLineItemMember.class, "SQL_GET_LINE_ITEM_MEMBER", hspSQL));
        lineItemMemberCacheLoader.addRelationship("memberSelections", lineItemMemberMbrSelIdKeyDef, transformParentIdCacheLoaderLI, dimSelIdKeyDef);
        lineItemMemberCache = new HspUpdateableCache<HspLineItemMember>(lineItemMemberCacheLoader, (HspSecDB)null, new CachedObjectKeyDef[] { lineItemMemberBaseDimIdKeyDef });

        resetCacheRoots();
        regenerateDimIndexCache();

        // User variable caches
        RelationshipCacheLoader<HspMbrSelection> userVarMbrSelCacheLoader =
            new RelationshipCacheLoader<HspMbrSelection>(new JDBCCacheLoader<HspMbrSelection>(HspMbrSelection.class, "SQL_GET_MBR_SELS_BY_TYPE", new String[] { Integer.toString(HspMbrSelection.TYPE_USER_VAR_MBR) }, hspSQL));
        userVarMbrSelCacheLoader.addRelationship("memberSelections", dimSelIdKeyDef, new JDBCCacheLoader<HspFormMember>(HspFormMember.class, "SQL_GET_MBR_SEL_ITEMS", hspSQL), FormMemberBySelIdKeyDef.FORM_MEMBER_SELECTION_BY_SEL_ID_KEY);
        OgnlTransformCacheLoader<HspMbrSelection> transformParentIdCacheLoader2 = new OgnlTransformCacheLoader<HspMbrSelection>(userVarMbrSelCacheLoader, HspMbrSelection.class, transformExpr);
        RelationshipCacheLoader<HspUserVariable> userVariableCacheLoader = new RelationshipCacheLoader<HspUserVariable>(new JDBCCacheLoader<HspUserVariable>(HspUserVariable.class, "SQL_GET_USER_VARIABLES", hspSQL));
        userVariableCacheLoader.addRelationship("memberSelection", userDefMbrSelKeyDef, transformParentIdCacheLoader2, dimSelIdKeyDef); //FormMemberBySelIdKeyDef.FORM_MEMBER_SELECTION_BY_SEL_ID_KEY);
        userVariableCache = new HspUpdateableCache<HspUserVariable>(userVariableCacheLoader, hspSecDB, new CachedObjectKeyDef[] { CachedObjectIdKeyDef.CACHED_OBJECT_ID_KEY, userVariableDimIdKeyDef, userVariableDimIdNameKeyDef });

        userVariableValueCache =
                new HspUpdateableCache<HspUserVariableValue>(new JDBCCacheLoader<HspUserVariableValue>(HspUserVariableValue.class, "SQL_GET_USER_VARIABLE_VALUES", hspSQL), hspSecDB, new CachedObjectKeyDef[] { userVariableValueUserIdVariableIdKeyDef, userVariableValueVariableIdKeyDef });

        RelationshipCacheLoader<HspEnumeration> enumCacheLoader = new RelationshipCacheLoader<HspEnumeration>(new JDBCCacheLoader<HspEnumeration>(HspEnumeration.class, "SQL_GET_ENUMERATIONS", hspSQL));
        enumCacheLoader.addRelationship("entries", enumKeyDef, new JDBCCacheLoader<HspEnumeration.Entry>(HspEnumeration.Entry.class, "SQL_GET_ENUMERATION_ENTRIES", hspSQL), enumEntryKeyDef);
        RelationshipCacheLoader<HspMbrSelection> enumMbrSelCacheLoader =
            new RelationshipCacheLoader<HspMbrSelection>(new JDBCCacheLoader<HspMbrSelection>(HspMbrSelection.class, "SQL_GET_MBR_SELS_BY_TYPE", new String[] { Integer.toString(HspMbrSelection.TYPE_ENUM_MBR) }, hspSQL));
        enumMbrSelCacheLoader.addRelationship("memberSelections", dimSelIdKeyDef, new JDBCCacheLoader<HspFormMember>(HspFormMember.class, "SQL_GET_MBR_SEL_ITEMS", hspSQL), FormMemberBySelIdKeyDef.FORM_MEMBER_SELECTION_BY_SEL_ID_KEY);
        OgnlTransformCacheLoader<HspMbrSelection> transformParentIdCacheLoader3 = new OgnlTransformCacheLoader<HspMbrSelection>(enumMbrSelCacheLoader, HspMbrSelection.class, transformExpr);
        enumCacheLoader.addRelationship("memberSelection", enumMbrSelKeyDef, transformParentIdCacheLoader3, dimSelIdKeyDef);
        CacheLoader<HspEnumeration> virtualEnumCacheLoader = new HspVirtualEnumCacheLoader(HspEnumeration.class, new HspSimpleVirtualEnumEntryGenerator(hspJS, hspAlsDB, internalSessionId), enumCacheLoader);
        enumCache = new HspUpdateableCache<HspEnumeration>(virtualEnumCacheLoader, null);
        generatedEnumCacheLoader = new HspGeneratedEnumCacheLoader();

        generatedEnumCacheLoader.registerEnumGenerator(HspPMState.class,
                                                       new HspDerivedEnumGenerator(HspDerivedEnumGenerator.createEnum(HspMDConstants.GENERATED_METRIC_PROCESS_STATE_ENUM_ID, "HSP_Metric_Process_State", "LABEL_METRIC_TYPE_PROCESS_STATE"), "stateId", "name", "name",
                                                                                   new Callable<Collection>() {
                    public Collection call() {
                        return hspPMDB.getPMStates(internalSessionId);
                    }
                }));

        generatedEnumCache = new GenericCache<HspEnumeration>(generatedEnumCacheLoader, null);

        udaCache = new HspUpdateableCache<HspUDA>(new JDBCCacheLoader<HspUDA>(HspUDA.class, "SQL_GET_UDAS", hspSQL), hspSecDB, new CachedObjectKeyDef[] { udaKeyDef, udaDimIdKeyDef, udaDimIdUDAValueKeyDef });
        udaBindingCache = new HspUpdateableCache<HspUDABinding>(new JDBCCacheLoader<HspUDABinding>(HspUDABinding.class, "SQL_GET_UDA_BINDINGS", hspSQL), hspSecDB, new CachedObjectKeyDef[] { udaBindingKeyDef, udaBindingsForMemberKeyDef, udaBindingsForUDAKeyDef });
        generatedCurrencyEnumsCache = new GenericCache<HspEnumeration>(new GeneratedCurrencyEnumCacheLoader(HspEnumeration.class, hspCurDB, hspJS.getSystemCfg(), internalSessionId), hspSecDB);
        generatedSmartListsEnumsCache = new GenericCache<HspEnumeration>(new GeneratedSmartListsEnumCacheLoader(HspEnumeration.class, this, internalSessionId), hspSecDB);
        generatedStaticMbrPropertiesEnumsCache = new GenericCache<HspEnumeration>(new GeneratedStaticPropertiesEnumsCacheLoader(HspEnumeration.class, this, hspCurDB, hspCalDB, internalSessionId), null);
        generatedFXTablesEnumsCache = new GenericCache<HspEnumeration>(new GeneratedFXTablesEnumCacheLoader(HspEnumeration.class, hspCurDB, internalSessionId), hspSecDB);
        generatedYearsEnumsCache = new GenericCache<HspEnumeration>(new GeneratedYearsEnumCacheLoader(HspEnumeration.class, hspCalDB, internalSessionId), hspSecDB);
        generatedSourcePlanTypeEnumsCache = new GenericCache<HspEnumeration>(new GeneratedAccountSourcePlanTypeCacheLoader(HspEnumeration.class, this, internalSessionId), hspSecDB);
        generatedPeriodsEnumsCache = new GenericCache<HspEnumeration>(new GeneratedPeriodsEnumCacheLoader(HspEnumeration.class, hspCalDB, internalSessionId), hspSecDB);
        generatedAtttribDimMbrsEnumCache = new GenericCache<HspEnumeration>(new GeneratedAttribDimMbrEnumCacheLoader(HspEnumeration.class, this, internalSessionId), hspSecDB);

        objectLockCacheLoader = new JDBCCacheLoader<HspLock>(HspLock.class, "SQL_GET_OBJECT_LOCK_ON_OBJECT", hspSQL);
        logger.exiting();
    }

    private synchronized void initEssbaseServerCache() {
        //TODO - fix defualtEssbase server to be reset if data source info got changed.
        List<HspExternalServer> defaultEssServer = new ArrayList<HspExternalServer>(1);
        defaultEssServer.add(getDefaultEssbaseServer());
        CacheLoader<HspExternalServer> essServerCacheLoader =
            new HspFieldDecryptorCacheLoader<HspExternalServer>(new UnionCacheLoader<HspExternalServer>(HspExternalServer.class, new CollectionCacheLoader<HspExternalServer>(defaultEssServer, HspExternalServer.class), new JDBCCacheLoader<HspExternalServer>(HspExternalServer.class,
                                                                                                                                                                                                                                                                 "SQL_GET_EXTERNAL_SERVERS",
                                                                                                                                                                                                                                                                 hspSQL)),
                                                                HspExternalServer.class, HspExternalServer.getEncryptedFieldNames());
        essbaseServerCache = new HspUpdateableCache<HspExternalServer>(essServerCacheLoader, hspSecDB, new CachedObjectKeyDef[] { displayNameKeyDef });
    }

    /**
     * Do NOT invoke save logic on default Essbase server.
     *
     * @return
     */
    private HspExternalServer getDefaultEssbaseServer() {
        HspDataSource ds = hspJS.getJSHome(internalSessionId).getDataSource(hspJS.getJSHome(internalSessionId).getApplicationEntry(hspJS.getAppName()).getDatasourceId());
        return HspExternalServer.createInstance(ds.getEssServer(), ds.getEssUser(), ds.getEssPassword(), false, true);
    }

    private synchronized void regenerateDimIndexCache() {
        Vector<HspDimension> dimensions = getBaseDimensions(HspConstants.PLAN_TYPE_ALL, internalSessionId);
        try {
            HspUtils.sortVector(dimensions, HspDimIdComparator.getStaticComparator());
        } catch (Exception e) {
            System.err.println("Error sorting dimensions for Supporting Detail");
            e.printStackTrace();
        }
        orderedDimIds = new int[dimensions.size()];
        for (int loop1 = 0; loop1 < orderedDimIds.length; loop1++) {
            HspDimension dim = dimensions.elementAt(loop1);
            orderedDimIds[loop1] = dim.getDimId();
        }
    }

    public HspMember createMember(int dimId) {
        HspDimension root = this.getDimRoot(dimId);
        if (root == null)
            throw new InvalidDimensionException(Integer.toString(dimId));


        return createMember(dimId, root.getObjectType());
    }

    public static HspMember createMember(int dimId, int objectType) {
        // Create the members with sensible property defaults when possible
        HspMember member = null;
        switch (objectType) {
        case HspConstants.kDimensionScenario:
            member = new HspScenario();
            break;
        case HspConstants.kDimensionVersion:
            member = new HspVersion();
            break;
        case HspConstants.kDimensionEntity:
            member = new HspEntity();
            break;
        case HspConstants.kDimensionAccount:
            member = new HspAccount();
            break;
        case HspConstants.kDimensionTimePeriod:
            member = new HspTimePeriod();
            break;
        case HspConstants.kDimensionYear:
            member = new HspYear();
            break;
        case HspConstants.gObjType_Currency:
            member = new HspCurrency();
            break;
        case HspConstants.gObjType_SimpleCurrency:
            member = new HspCurrency();
            break;
        case HspConstants.gObjType_ExternalMember:
        case HspConstants.gObjType_UserDefinedMember:
        case HspConstants.gObjType_BudgetRequest:
        case HspConstants.gObjType_BudgetRequestASO:
        case HspConstants.gObjType_DecisionPackageASO:
            member = new HspMember();
            break;
        case HspConstants.gObjType_AttributeMember:
        case HspConstants.gObjType_AttributeDim:
            member = new HspAttributeMember();
            break;
        case HspConstants.gObjType_ReplacementDimension:
        case HspConstants.gObjType_ReplacementMember:
            member = new HspReplacementMember();
            break;
        case HspConstants.gObjType_Metric:
            member = new HspMetric();
            break;
        case HspConstants.gObjType_PMDimension:
        case HspConstants.gObjType_PMDimMember:
            member = new HspPMMember();
            break;
        case HspConstants.gObjType_DPDimension:
        case HspConstants.gObjType_DPDimMember:
            member = new HspDPMember();
            break;

        }

        if (member == null) {
            InvalidDimensionException x = new InvalidDimensionException(Integer.toString(dimId));
            logger.throwing(x);
            throw x;
        }
        member.setDimId(dimId);
        member.setParentId(dimId);
        member.setObjectType(objectType);
        // See particular class for other property defaults

        return member;
    }
    // create a user defined dimension with some sensible properties ASO cubes enabled by default

    public HspDimension createUserDefinedDimension(String dimensionName, int sessionId) {
        return createUserDefinedDimensionImpl(dimensionName, true, sessionId);
    }
    // create a user defined dimension with some sensible properties ASO cubes enabled or not by argument

    public HspDimension createUserDefinedDimension(String dimensionName, boolean enableUserDimForAso, int sessionId) {
        return createUserDefinedDimensionImpl(dimensionName, enableUserDimForAso, sessionId);
    }
    // create a user defined dimension with some sensible properties

    private HspDimension createUserDefinedDimensionImpl(String dimensionName, boolean enableUserDimForAso, int sessionId) {
        logger.entering(dimensionName);
        HspDimension dimension = new HspDimension();
        dimension.setObjectName(dimensionName);
        dimension.setUsedIn(getUsedInForAllCubes(!enableUserDimForAso, sessionId));
        dimension.setEnforceSecurity(false);
        dimension.setDataStorage(HspConstants.kDataStorageStoreData);
        dimension.setObjectType(HspConstants.gObjType_UserDefinedMember);
        dimension.setDimType(HspConstants.kDimTypeUser);
        dimension.setParentId(HspConstants.gFolder_Dimensions);
        dimension.setTwopassCalc(false);
        dimension.setDensity(HspConstants.PLAN_TYPE_ALL, HspConstants.kDataDensitySparse);
        dimension.setConsolOp(HspConstants.PLAN_TYPE_ALL, HspConstants.kDataConsolIgnore);
        dimension.setDimEditor(true);
        logger.exiting(dimension);
        return dimension;
    }

    private int getUsedInForAllCubes(int sessionId) {
        return getUsedInForAllCubesImpl(false, sessionId);
    }

    public int getUsedInForAllCubes(boolean excludeASOCubes, int sessionId) {
        return getUsedInForAllCubesImpl(excludeASOCubes, sessionId);
    }

    private int getUsedInForAllCubesImpl(boolean excludeASOCubes, int sessionId) {
        int validPlanTypes = HspConstants.PLAN_TYPE_NONE;
        Vector<HspCube> cubes = getCubes(sessionId);
        if (cubes != null) {
            for (int loop1 = 0; loop1 < cubes.size(); loop1++) {
                HspCube cube = cubes.elementAt(loop1);
                if ((cube == null) || (excludeASOCubes && cube.isASOCube()))
                    continue;
                validPlanTypes = validPlanTypes | cube.getPlanType();
            }
        }
        return (validPlanTypes);
    }

    /**
     * This recreates the memberCacheHash, adding or removing any changed dimensions,
     * and also updates the roots of the dimensions, incase their properties or names
     * have changed.
     */
    private void resetCacheRoots() {
        synchronized (memberCacheHash) {
            Hashtable<Integer, GenericCache<? extends HspMember>> oldMemberCacheHash = (Hashtable<Integer, GenericCache<? extends HspMember>>)memberCacheHash.clone();
            memberCacheHash.clear();
            Vector<HspDimension> dimensions = dimensionCache.getUnfilteredCache();
            dimensions.addAll(hiddenDimensionsCache.getUnfilteredCache());
            dimensions.addAll(metricDimensionCache.getUnfilteredCache());
            dimensions.addAll(replacementDimensionCache.getUnfilteredCache());
            dimensions.addAll(pmDimensionCache.getUnfilteredCache());
            dimensions.addAll(dpDimensionCache.getUnfilteredCache());
            dimensions.addAll(virtualDimensionsCache.getUnfilteredCache());
            //dimensions.addAll(brDimensionCache.getUnfilteredCache());

            for (int loop1 = 0; loop1 < dimensions.size(); loop1++) {
                HspDimension dim = dimensions.elementAt(loop1);
                if (dim != null) {
                    //Create the Root
                    HspMember root = createRootMemberFromDimension(dim);

                    //Create the cache;
                    GenericCache dimCache;
                    Integer key = dim.getId();
                    switch (dim.getId()) {
                    case HspConstants.kDimensionScenario:
                        dimCache = scenarioCache;
                        //                    if(dimCache instanceof HspUpdateableCache)
                        //                        ((HspUpdateableCache)dimCache).setCacheReOrderedEventHandler(hspFMDB);//new HspCacheReOrderedEventHandler(hspFMDB.getVCRulesProcessor()));
                        break;
                    case HspConstants.kDimensionVersion:
                        dimCache = versionCache;
                        break;
                    case HspConstants.kDimensionEntity:
                        dimCache = entityCache;
                        int defaultCurrencyId = 0;
                        HspSystemCfg cfg = hspJS.getSystemCfg();
                        if (cfg != null)
                            defaultCurrencyId = cfg.getDefCurId();
                        ((HspEntity)root).setDefaultCurrency(defaultCurrencyId);
                        break;
                    case HspConstants.kDimensionAccount:
                        dimCache = accountCache;
                        break;
                    case HspConstants.kDimensionTimePeriod:
                        dimCache = timePeriodCache;
                        break;
                    case HspConstants.kDimensionYear:
                        dimCache = yearCache;
                        break;
                        // case HspConstants.kDimensionSimpleCurrency:
                    case HspConstants.kDimensionCurrency:
                        dimCache = currencyCache;
                        break;
                    case HspConstants.kDimensionMetric:
                        dimCache = metricCache;
                        break;
                    default:
                        if (dim.getDimId() == HspConstants.kDimensionRates)
                            dimCache =
                                    new HspUpdateableCache<HspMember>(new CollectionCacheLoader<HspMember>(Arrays.asList(new HspMember[] { (HspMember)HspConstants.MEMBER_HSP_INPUT_VALUE.cloneForUpdate(), (HspMember)HspConstants.MEMBER_HSP_INPUT_CURRENCY.cloneForUpdate() }), HspMember.class),
                                                                      hspSecDB, new CachedObjectKeyDef[] { orderedObjectIdKeyDef, cachedObjectDefaultKeyDef, memberPsIdKeyDef });
                        else if (dim.getDimId() == HspConstants.kDimensionMetaData) {

                            HspMDMember[] mbrs = MDMember.MD_ALL_VIRTUAL_MEMBERS_TREE;
                            mbrs = HspUtils.concatArrays(mbrs, MDMember.MD_MEMBER_DYNAM_CHILDREN);

                            ArrayList<HspMDMember> attrMDMbrs = MDUtils.getAttribDimensionMbrs(dimensions);
                            /*int pos=80000;
                        //TODO:: encapsulate as function position and  attrib id
                        int attribDimMbrId= HspMDConstants.MD_MBR_ID_END+1;
                        ArrayList<HspMDMember> attrMDMbrs = new ArrayList<HspMDMember>();
                        for( HspDimension curDim :dimensions){
                          if(curDim.getDimType()== HspConstants.kDimTypeAttribute){
                            HspMDMember mdMember = new HspMDMember();
                            mdMember.setObjectId(attribDimMbrId++);
                            mdMember.setObjectName("attribDimMember"+ curDim.getName());
                            mdMember.setSQLName("attribDimMember");
                            mdMember.setPosition(pos++);
                            mdMember.setHALLoadName(curDim.getName());
                            mdMember.setDataType(HspConstants.DATA_TYPE_ENUMERATION);
                            mdMember.setEnumerationId(curDim.getDimId());
                            mdMember.setLabel(curDim.getName());
                            mdMember.setPropertyType(HspMDConstants.MD_PROPERTY_TYPE_ATTRIBUTE_MEMBER);
                            mdMember.makeReadOnly();
                            attrMDMbrs.add(mdMember);
                          }
                        }*/
                            if (!HspUtils.isNullOrEmpty(attrMDMbrs)) {
                                mbrs = HspUtils.concatArrays(mbrs, attrMDMbrs.toArray(new HspMDMember[attrMDMbrs.size()]));
                            }

                            if (cubeCache != null) {

                                int asoCount = 0;
                                Vector<HspCube> cubes = cubeCache.getUnfilteredCache();

                                for (int cubeIndex = 0; cubeIndex < cubes.size(); cubeIndex++) {
                                    if (cubes.get(cubeIndex).getType() == HspConstants.ASO_CUBE) {
                                        asoCount++;
                                    }
                                }

                                if (asoCount > 0) {
                                    mbrs = HspUtils.concatArrays(mbrs, new HspMDMember[] { MDMember.MD_MEMBER_ASO_HYERARCHY_TYPE });
                                    for (int n = 0; n < asoCount; n++) {
                                        mbrs = HspUtils.concatArrays(mbrs, new HspMDMember[] { MDMember.MD_MEMBER_SOLVE_ORDER[n] });
                                    }
                                }
                            }

                            List<HspMDMember> mdMbrs = Arrays.asList(mbrs);
                            dimCache = new HspUpdateableCache<HspMDMember>(new CollectionCacheLoader<HspMDMember>(mdMbrs, HspMDMember.class), hspSecDB, new CachedObjectKeyDef[] { orderedObjectIdKeyDef, cachedObjectDefaultKeyDef, memberPsIdKeyDef });
                        } else if ((dim.getObjectType() == HspConstants.gObjType_AttributeMember) || (dim.getObjectType() == HspConstants.gObjType_AttributeDim)) { //System.out.println("Creating new cache for attrib dim: " + dim.getMemberName());
                            dimCache =
                                    new HspUpdateableCache<HspAttributeMember>(new JDBCCacheLoader<HspAttributeMember>(HspAttributeMember.class, "SQL_GET_ATTR_DIM_MEMBERS", new String[] { Integer.toString(dim.getId()) }, hspSQL), hspSecDB, new CachedObjectKeyDef[] { orderedObjectIdKeyDef,
                                                                                                                                                                                                                                                                           cachedObjectDefaultKeyDef,
                                                                                                                                                                                                                                                                           memberPsIdKeyDef });
                        } else if (dim.getObjectType() == HspConstants.gObjType_PMDimension || dim.getObjectType() == HspConstants.gObjType_PMDimMember) {
                            try {
                                dimCache = oldMemberCacheHash.get(dim.getDimId());
                            } catch (Exception e) {
                                dimCache = null;
                            }
                            if (dimCache == null) { // || !dimCache.getObject(dim.getDimId()).getName().equalsIgnoreCase(dim.getName())) {
                                // roots comparison is added to fix bug
                                ///Bug 16775448 - GOT ERROR WHEN TRY TO ACCESS PUH IN SV AFTER RENAMING PUH IN PLANNING WEB
                                String[] params = new String[] { String.valueOf(dim.getDimId()) };
                                RelationshipCacheLoader<HspPMMember> pmMemberCacheLoader = new RelationshipCacheLoader<HspPMMember>(new JDBCCacheLoader<HspPMMember>(HspPMMember.class, "SQL_GET_PM_MEMBERS_FOR_DIMENSION", params, hspSQL));
                                PMMemberOwnerKeyDef memberOwnerKeyDef = PMMemberOwnerKeyDef.PM_MEMBER_OWNER_NON_UNIQUE_KEY_DEF;
                                PMMemberByPMDimIdPrimaryIdSecondaryIdKeyDef pmMemberByPMDimIdByPrimaryIdSecondaryIdKeyDef = PMMemberByPMDimIdPrimaryIdSecondaryIdKeyDef.PM_MEMBER_BY_PMDIM_ID_PRIMARY_ID_SECONDARY_ID_KEY_DEF;

                                pmMemberCacheLoader.addRelationship("preReviewerIds", pmMemberByPMDimIdByPrimaryIdSecondaryIdKeyDef, new JDBCCacheLoader<HspPMMemberOwner>(HspPMMemberOwner.class, "SQL_GET_PM_MEMBER_OWNERS_PRE_REVIEW", params, hspSQL), memberOwnerKeyDef,
                                                                    FieldKeyDef.createKeyDef(HspPMMemberOwner.class, "pmOwnerId", false));
                                pmMemberCacheLoader.addRelationship("postReviewerIds", pmMemberByPMDimIdByPrimaryIdSecondaryIdKeyDef, new JDBCCacheLoader<HspPMMemberOwner>(HspPMMemberOwner.class, "SQL_GET_PM_MEMBER_OWNERS_POST_REVIEW", params, hspSQL), memberOwnerKeyDef,
                                                                    FieldKeyDef.createKeyDef(HspPMMemberOwner.class, "pmOwnerId", false));
                                pmMemberCacheLoader.addRelationship("notifieeIds", pmMemberByPMDimIdByPrimaryIdSecondaryIdKeyDef, new JDBCCacheLoader<HspPMMemberOwner>(HspPMMemberOwner.class, "SQL_GET_PM_MEMBER_OWNERS_NOTIFIEE", params, hspSQL), memberOwnerKeyDef,
                                                                    FieldKeyDef.createKeyDef(HspPMMemberOwner.class, "pmOwnerId", false));

                                // secclassId is used to relate the memberSelections to its parent HspMbrSelection (selId), after the parent child
                                // relationship is created by the RelationshipCacheLoader, set the objdefId back to -1;
                                String transformExpr = "memberSelections == null ? null : memberSelections.{secclassId = -1}";

                                String[] mbrSelectionParams = { String.valueOf(HspMbrSelection.TYPE_PU_VALUE_DEFN) };
                                RelationshipCacheLoader<HspMbrSelection> mbrSelCacheLoader = new RelationshipCacheLoader<HspMbrSelection>(new JDBCCacheLoader<HspMbrSelection>(HspMbrSelection.class, "SQL_GET_MBR_SELS_BY_TYPE", mbrSelectionParams, hspSQL));
                                mbrSelCacheLoader.addRelationship("memberSelections", CachedObjectIdKeyDef.CACHED_OBJECT_ID_KEY, new JDBCCacheLoader<HspFormMember>(HspFormMember.class, "SQL_GET_MBR_SEL_ITEMS", hspSQL),
                                                                  FormMemberBySelIdKeyDef.FORM_MEMBER_SELECTION_BY_SEL_ID_KEY);
                                OgnlTransformCacheLoader<HspMbrSelection> transformParentIdCacheLoader = new OgnlTransformCacheLoader<HspMbrSelection>(mbrSelCacheLoader, HspMbrSelection.class, transformExpr);
                                pmMemberCacheLoader.addRelationship("valueDefnMbrSel", FieldKeyDef.createKeyDef(HspPMMember.class, "valueDefnMbrSelId", true), transformParentIdCacheLoader, CachedObjectIdKeyDef.CACHED_OBJECT_ID_KEY);

                                pmMemberCacheLoader.addRelationship("naturalOwnerId", pmMemberByPMDimIdByPrimaryIdSecondaryIdKeyDef, new JDBCCacheLoader<HspPMMemberOwner>(HspPMMemberOwner.class, "SQL_GET_PM_MEMBER_OWNERS_NAT_OWNER", params, hspSQL),
                                                                    PMMemberOwnerKeyDef.PM_MEMBER_OWNER_UNIQUE_KEY_DEF, FieldKeyDef.createKeyDef(HspPMMemberOwner.class, "pmOwnerId", true));
                                dimCache = new HspUpdateableCache<HspPMMember>(pmMemberCacheLoader, hspSecDB);
                            }
                        } else if (dim.getObjectType() == HspConstants.gObjType_DPDimension || dim.getObjectType() == HspConstants.gObjType_DPDimMember) {
                            // System.out.println("Here for " + dim.getObjectName() + " type: " + dim.getObjectType() + " dimension: " + dim.getDimId() + " id:" + dim.getId());
                            try {
                                dimCache = oldMemberCacheHash.get(dim.getDimId());
                            } catch (Exception e) {
                                dimCache = null;
                            }
                            if (dimCache == null) {
                                String[] params = new String[] { String.valueOf(dim.getDimId()) };
                                RelationshipCacheLoader<HspDPMember> dpMemberCacheLoader = new RelationshipCacheLoader<HspDPMember>(new JDBCCacheLoader<HspDPMember>(HspDPMember.class, "SQL_GET_DP_MEMBERS_FOR_DIMENSION", params, hspSQL));
                                HspDPMemberKeyDef dpMemberByIdKeyDef = HspDPMemberKeyDef.DP_MEMBER__KEY_DEF;
                                DPMemberAttributeKeyDef dpMemberAttributeKeyDef = DPMemberAttributeKeyDef.DP_REVIEWER_DP_MEMBER_ID_KEY_DEF;
                                dpMemberCacheLoader.addRelationship("sharingEntityIds", dpMemberByIdKeyDef, new JDBCCacheLoader<HspDPSharingEntity>(HspDPSharingEntity.class, "SQL_GET_DP_SHARING_ENTITIES", null, hspSQL), HspDPSharingEntitiesKeyDef.DP_ENTITY_DP_MEMBER_ID_KEY_DEF,
                                                                    FieldKeyDef.createKeyDef(HspDPSharingEntity.class, "entityId", false));
                                dpMemberCacheLoader.addRelationship("reviewerIds", dpMemberByIdKeyDef, new JDBCCacheLoader<HspDPReviewer>(HspDPReviewer.class, "SQL_GET_DP_REVIEWERS", null, hspSQL), HspDPReviewerKeyDef.DP_REVIEWER_DP_MEMBER_ID_KEY_DEF,
                                                                    FieldKeyDef.createKeyDef(HspDPReviewer.class, "reviewerId", false));
                                dpMemberCacheLoader.addRelationship("attributes", dpMemberByIdKeyDef, new JDBCCacheLoader<HspDPMemberAttribute>(HspDPMemberAttribute.class, "SQL_GET_DP_MEMBER_ATTRIBUTES", hspSQL), dpMemberAttributeKeyDef);
                                dimCache = new HspUpdateableCache<HspDPMember>(dpMemberCacheLoader, hspSecDB);
                            }
                        } else if (dim.getObjectType() == HspConstants.gObjType_ReplacementDimension || dim.getObjectType() == HspConstants.gObjType_ReplacementMember) {
                            try {
                                dimCache = oldMemberCacheHash.get(dim.getDimId());
                            } catch (Exception e) {
                                dimCache = null;
                            }
                            if (dimCache == null) {
                                dimCache =
                                        new HspUpdateableCache<HspReplacementMember>(new JDBCCacheLoader<HspReplacementMember>(HspReplacementMember.class, "SQL_GET_REPLACEMENT_MEMBERS_FOR_DIMENSION", new String[] { Integer.toString(dim.getId()) }, hspSQL), hspSecDB, new CachedObjectKeyDef[] { orderedObjectIdKeyDef,
                                                                                                                                                                                                                                                                                                      cachedObjectDefaultKeyDef,
                                                                                                                                                                                                                                                                                                      memberBaseMemberIdKeyDef });
                            }
                        } else {
                            //System.out.println("Creating new cache for cust dim: " + dim.getMemberName());
                            try {
                                dimCache = oldMemberCacheHash.get(dim.getDimId());
                            } catch (Exception e) {
                                dimCache = null;
                            }
                            if (dimCache == null) {
                                dimCache =
                                        new HspUpdateableCache<HspMember>(new JDBCCacheLoader<HspMember>(HspMember.class, "SQL_GET_CUST_DIM_MEMBERS", new String[] { Integer.toString(dim.getId()) }, hspSQL), hspSecDB, new CachedObjectKeyDef[] { orderedObjectIdKeyDef, cachedObjectDefaultKeyDef,
                                                                                                                                                                                                                                                    memberBaseMemberIdKeyDef,
                                                                                                                                                                                                                                                    memberPsIdKeyDef,
                                                                                                                                                                                                                                                    hspObjectOldNameKeyDef });
                            }
                            //                        RelationshipCacheLoader<HspMember> dimCacheLoader = new RelationshipCacheLoader<HspMember>(new JDBCCacheLoader<HspMember>(HspMember.class, "SQL_GET_CUST_DIM_MEMBERS", new String[] { Integer.toString(dim.getId()) }, hspSQL));
                            //                        dimCacheLoader.addRelationship("memberOnFlyDetail", orderedObjectIdKeyDef, new JDBCCacheLoader<HspMemberOnFlyDetail>(HspMemberOnFlyDetail.class, "SQL_GET_MEMBER_ON_FLY_DETAIL", hspSQL), mbrOnFlyParentIdKeyDef);
                            //                        dimCache = new HspUpdateableCache<HspMember>(dimCacheLoader, hspSecDB, new CachedObjectKeyDef[] { orderedObjectIdKeyDef, cachedObjectDefaultKeyDef, memberBaseMemberIdKeyDef, memberPsIdKeyDef, mbrIsDynamicChildEnabledKeyDef, hspObjectOldNameKeyDef });
                        }
                    }
                    //Set the cache's root
                    dimCache.setRoot(root);
                    if (dimCache instanceof HspUpdateableCache &&
                        !(dim.getObjectType() == HspConstants.gObjType_DPDimension || dim.getObjectType() == HspConstants.gObjType_DPDimMember || dim.getObjectType() == HspConstants.gObjType_PMDimension || dim.getObjectType() == HspConstants.gObjType_PMDimMember ||
                          (dim.getObjectType() == HspConstants.gObjType_AttributeMember) || (dim.getObjectType() == HspConstants.gObjType_AttributeDim) || dim.getDimId() == HspConstants.kDimensionMetaData))
                        ((HspUpdateableCache)dimCache).setCacheReOrderedEventHandler(hspFMDB);

                    memberCacheHash.put(key, dimCache);
                }
            }
            updatePlanTypeLockOpts();
        }
    }
    /*
	private synchronized void resetCacheRoots()
	{
		//TODO: Fix "resetCacheRoots()" to work on all dimensions, not just accts/entities
		HspDimension root;
		//Account cache
		root = getDimRoot(HspConstants.gObjDim_Account);
		HspAccount account = new HspAccount();
		account.setObjectId(root.getId());
		account.setObjectName(root.getName());
		account.setDimId(root.getDimId());
		accountCache.setRoot(account);
		//Entity Cache
		root = getDimRoot(HspConstants.gObjDim_Entity);
		HspEntity entity = new HspEntity();
		entity.setObjectId(root.getId());
		entity.setObjectName(root.getName());
		entity.setDimId(root.getDimId());
		entityCache.setRoot(entity);
		rebuildMemberCacheHash();
	}
	private synchronized void rebuildMemberCacheHash()
	{
		memberCacheHash = new Hashtable();
		Vector dimensions = dimensionCache.getUnfilteredCache();
		if (dimensions != null)
		{
			for (int loop1=0;loop1<dimensions.size();loop1++)
			{
				HspUpdateableCache dimCache = null;
				HspDimension dim = (HspDimension) dimensions.elementAt(loop1);
				Integer key = new Integer(dim.getId());
				switch (dim.getId())
				{
				case HspConstants.kDimensionScenario:
					dimCache = scenarioCache;
					break;
				case HspConstants.kDimensionVersion:
					dimCache = versionCache;
					break;
				case HspConstants.kDimensionEntity:
					dimCache = entityCache;
					break;
				case HspConstants.kDimensionAccount:
					dimCache = accountCache;
					break;
				case HspConstants.kDimensionTimePeriod:
					dimCache = timePeriodCache;
					break;
				case HspConstants.kDimensionYear:
					dimCache = yearCache;
					break;
				case HspConstants.kDimensionCurrency:
					dimCache = currencyCache;
					break;
				default:
					dimCache = new HspUpdateableCache(HspMember.class, "SQL_GET_CUST_DIM_MEMBERS", new String[]{Integer.toString(dim.getId())}, hspSQL, hspSecDB);
				}
				memberCacheHash.put(key, dimCache);
			}
		}
	}
	*/
    //Bulk read methods

    public Vector<HspDimension> getAllDimensions(int planTypes, int sessionId) {
        hspStateMgr.verify(sessionId);
        Vector<HspDimension> allDimensions = dimensionCache.getUnfilteredCache();
        if ((allDimensions == null) || (allDimensions.size() <= 0))
            return allDimensions;
        Vector<HspDimension> retDimensions = new Vector<HspDimension>();
        for (int loop1 = 0; loop1 < allDimensions.size(); loop1++) {
            HspDimension dim = allDimensions.elementAt(loop1);
            //If the dimention is not null, and is used in at least one of the plan types we care about, then we add it to the list.
            if ((dim != null) && ((dim.getUsedIn() & planTypes) > 0))
                retDimensions.addElement(dim);
        }
        return retDimensions;
    }

    /**
     * Invalidates Reporting application/cube Dimension/Member caches so that any modification can be available for subsequent cache
     * load.
     *
     * @param   essServer	HspEssbaseServer object to be used for Essbase connection.
     * @param   appName		Essbase application name.
     * @param   cubeName	Essbase Cube name.
     * @param   sessionId	Identifies the caller for security purposes
     * @throws Exception
     */
    public void invalidateReportingCubeCaches(HspExternalServer essServer, String appName, String cubeName, int sessionId) throws Exception {
        hspStateMgr.verify(sessionId);
        hspRepDimMbrCacheMgr.invalidateReportingCubeCaches(essServer, appName, cubeName);
    }

    /**
     * Returns a Vector of all <code>HspDimension</code> objects for the given Essbase server, app name and cube name combination.
     *
     * @param   essServer	HspEssbaseServer object to be used for Essbase connection.
     * @param   appName		Essbase application name.
     * @param   cubeName	Essbase Cube name.
     * @return	Vector		of <code>HspDimension</code> objects.
     */
    public Vector<HspDimension> getReportingCubeDimensions(HspExternalServer essServer, String appName, String cubeName, int sessionId) {
        hspStateMgr.verify(sessionId);
        return hspRepDimMbrCacheMgr.getReportingCubeDimensions(essServer, appName, cubeName);
    }

    /**
     * Returns <code>HspDimension</code> object for the given dimName on specified Essbase server,
     * app name and cube name combination.
     *
     * @param   essServer	HspEssbaseServer object to be used for Essbase connection.
     * @param   appName		Essbase application name.
     * @param   cubeName	Essbase Cube name.
     * @param   dimName		Essbase Dimension name.
     * @return	HspDimension
     */
    public HspDimension getReportingCubeDimension(HspExternalServer essServer, String appName, String cubeName, String dimName, int sessionId) {
        hspStateMgr.verify(sessionId);
        return hspRepDimMbrCacheMgr.getReportingCubeDimension(essServer, appName, cubeName, dimName);
    }

    /**
     * Checks whether dimId belongs to a Reporting application Dimension or not.
     *
     * @param   dimId		Reporting dimension Id.
     * @param   sessionId	Identifies the caller for security purposes
     * @return	boolean
     */
    public boolean isReportingCubeDimension(int dimId, int sessionId) {
        hspStateMgr.verify(sessionId);
        return hspRepDimMbrCacheMgr.isReportingCubeDimension(dimId);
    }

    /**
     * Returns <code>String</code> array of Substitution Variables and their values for Reporting application associated with
     * given dimension id.
     *
     * @param   dimId		Reporting dimension Id.
     * @param   filterDuplicates		remove duplicate variables.
     * @param   sessionId	Identifies the caller for security purposes
     * @return	String[][]
     * @throws Exception
     */
    public String[][] getReportingSubstitutionVariables(int dimId, boolean filterDuplicates, int sessionId) throws Exception {
        hspStateMgr.verify(sessionId);
        return hspRepDimMbrCacheMgr.getReportingSubstitutionVariables(dimId, filterDuplicates);
    }

    //Returns the dimension that has a dimension Id of 30

    public Vector<HspDimension> getHiddenDimensions(int planType, int sessionId) {
        hspStateMgr.verify(sessionId);
        Vector<HspDimension> hiddenDimensions = hiddenDimensionsCache.getUnfilteredCache();
        if ((hiddenDimensions == null) || (hiddenDimensions.size() <= 0))
            return hiddenDimensions;
        Vector<HspDimension> retDimensions = new Vector<HspDimension>();
        for (int loop1 = 0; loop1 < hiddenDimensions.size(); loop1++) {
            HspDimension dim = hiddenDimensions.elementAt(loop1);
            //If the dimention is not null, then we add it to the list.
            if ((dim != null) && ((dim.getUsedIn() & planType) > 0))
                retDimensions.addElement(dim);
        }
        return retDimensions;
    }

    public Vector<HspDimension> getBaseDimensions(int planTypes, int sessionId) {
        //TODO: Put back original implementation
        return getBaseDimensions(planTypes, false, sessionId); // make this false later
        //return getBaseDimensions(planTypes, false, true, getReplacementDimensions(planTypes, sessionId), sessionId);
    }

    public Vector<HspDimension> getBaseDimensions(int planTypes, boolean includeAttrDims, int sessionId) {
        //TODO: Put back original implementation
        return getBaseDimensions(planTypes, includeAttrDims, false, null, sessionId);
        //return getBaseDimensions(planTypes, true, true, getReplacementDimensions(planTypes, sessionId), sessionId);
    }

    public Vector<HspDimension> getBaseDimensions(int planTypes, boolean includeAttrDims, boolean includeMetricDim, List<HspReplacementDimension> replacementDims, int sessionId) {
        hspStateMgr.verify(sessionId);
        Vector<HspDimension> allDimensions = dimensionCache.getUnfilteredCache();
        if ((allDimensions == null) || (allDimensions.size() <= 0))
            return allDimensions;

        Set<Integer> replacedDimIdSet = Collections.emptySet();
        if (replacementDims != null) {
            replacedDimIdSet = new HashSet<Integer>();
            for (Iterator<HspReplacementDimension> it = replacementDims.iterator(); it.hasNext(); ) {
                HspReplacementDimension replacementDim = it.next();
                if (replacementDim == null)
                    throw new IllegalArgumentException("replacementDims contains null replacement dimensions.");
                int[] dimIds = replacementDim.getDimIds();
                if (dimIds != null) {
                    for (int dimId : dimIds) {
                        if (dimId > 0 && !replacedDimIdSet.add(dimId))
                            throw new IllegalArgumentException("More than one replacementDimension is replacing the same dimension: " + dimId);
                    }
                }
            }
        }

        HspSystemCfg sysCfg = hspJS.getSystemCfg();
        Vector<HspDimension> baseDimensions = new Vector<HspDimension>();
        for (HspDimension dim : allDimensions) {
            if (dim == null)
                continue;

            // Dont include currency dim if we have a single currency app
            if ((dim != null) && (dim.getDimId() == HspConstants.kDimensionCurrency) && !sysCfg.isMultiCurrency() && !sysCfg.isSimpleMultiCurrency())
                continue;

            // Metric will be aded at the end if requested.
            if (dim.getDimType() == HspConstants.kDimTypeMetric)
                continue;

            //Don't include Hsp_View dim if usedin is 0
            if (dim != null && (dim.getDimId() == HspConstants.kDimensionView) && dim.getUsedIn() == 0)
                continue;

            //If the dimention is not null, and is not an Attribute dimention, then it is a base dimention
            //If it is used in at least one of the plan types we care about, then we add it to the list.
            if ((dim.getDimType() != HspConstants.kDimTypeAttribute || includeAttrDims) && ((dim.getUsedIn() & planTypes) > 0) && !replacedDimIdSet.contains(dim.getId()))
                baseDimensions.addElement(dim);

        }

        // Check for external reporting cube
        if (baseDimensions.size() < 1) {
            HspCube essCube = getEssbaseCubeByPlanType(planTypes);
            if (essCube != null) {
                baseDimensions = getReportingCubeDimensions(getEssbaseServer(essCube.getEssbaseServerId(), sessionId), essCube.getAppName(), essCube.getCubeName(), sessionId);
            }
        }

        if (includeMetricDim) {
            HspDimension metricDimension = getDimRoot(HspConstants.kDimensionMetric);
            if (metricDimension != null)
                baseDimensions.add(metricDimension);
        }

        if (replacementDims != null)
            baseDimensions.addAll(replacementDims);

        return baseDimensions;
    }

    public Vector<HspDimension> getBaseDimensions(int sessionId) {
        hspStateMgr.verify(sessionId);
        Vector<HspDimension> allDimensions = dimensionCache.getUnfilteredCache();
        if ((allDimensions == null) || (allDimensions.size() <= 0))
            return allDimensions;

        HspSystemCfg sysCfg = hspJS.getSystemCfg();
        Vector<HspDimension> baseDimensions = new Vector<HspDimension>();
        for (int loop1 = 0; loop1 < allDimensions.size(); loop1++) {
            HspDimension dim = allDimensions.elementAt(loop1);

            // Dont include currency dim if we have a single currency app
            if ((dim != null) && (dim.getDimId() == HspConstants.kDimensionCurrency) && !sysCfg.isMultiCurrency() && !sysCfg.isSimpleMultiCurrency())
                continue;
            //Don't include Hsp_View dim if usedin is 0
            if (dim != null && (dim.getDimId() == HspConstants.kDimensionView) && dim.getUsedIn() == 0)
                continue;
            //If the dimention is not null, and is not an Attribute dimention, then it is a base dimention
            //If it is used in at least one of the plan types we care about, then we add it to the list.
            if ((dim != null) && (dim.getDimType() != HspConstants.kDimTypeAttribute)) {
                baseDimensions.addElement(dim);
            }
        }
        return baseDimensions;
    }


    public Vector<Integer> getInvalidDimensions(int planType, int sessionId) {
        Vector<HspDimension> allDimensions = dimensionCache.getUnfilteredCache();
        Vector<Integer> invalidDimensions = new Vector<Integer>();
        if ((allDimensions == null) || (allDimensions.size() <= 0))
            return invalidDimensions;
        for (int loop1 = 0; loop1 < allDimensions.size(); loop1++) {
            HspDimension dim = allDimensions.elementAt(loop1);
            //If the dimention is not null, and is not an Attribute dimention, then it is a base dimention
            //If it is used in at least one of the plan types we care about, then we add it to the list.
            if ((dim != null) && (dim.getObjectType() == HspConstants.gObjType_UserDefinedMember) && ((dim.getUsedIn() & planType) != planType))
                invalidDimensions.addElement(dim.getDimId());
        }
        return invalidDimensions;
    }

    public Vector getCustomDimensions(int planTypes, int sessionId) {
        hspStateMgr.verify(sessionId);
        return getCustomDimensionsInternal(planTypes);
    }

    private Vector getCustomDimensionsInternal(int planTypes) {
        Vector allDimensions = dimensionCache.getUnfilteredCache();
        if ((allDimensions == null) || (allDimensions.size() <= 0))
            return allDimensions;
        Vector baseDimensions = new Vector();
        for (int loop1 = 0; loop1 < allDimensions.size(); loop1++) {
            HspDimension dim = (HspDimension)allDimensions.elementAt(loop1);
            //If the dimention is not null, and is not an Attribute dimention, then it is a base dimention
            //If it is used in at least one of the plan types we care about, then we add it to the list.
            if ((dim != null) && (dim.getObjectType() == HspConstants.gObjType_UserDefinedMember) && ((dim.getUsedIn() & planTypes) > 0))
                baseDimensions.addElement(dim);

        }
        return baseDimensions;
    }

    public Vector<HspAttributeDimension> getAttributeDimensions(int sessionId) {
        hspStateMgr.verify(sessionId);
        return attributeDimensionCache.getUnfilteredCache();
    }

    private Vector<HspAttributeDimension> getAttributeDimensionsInternal() {
        return attributeDimensionCache.getUnfilteredCache();
    }

    public Vector<HspAttributeDimension> getAttributeDimensionsForBaseDim(int dimId) {
        Vector<HspAttributeDimension> attributeDimensions = attributeDimensionCache.getUnfilteredCache();
        Vector<HspAttributeDimension> attrDimsForDim = null;
        HspAttributeDimension aDim = null;
        if (attributeDimensions != null) {
            for (int i = 0; i < attributeDimensions.size(); i++) {
                aDim = attributeDimensions.elementAt(i);
                if (aDim.baseDimId == dimId) {
                    if (attrDimsForDim == null) {
                        attrDimsForDim = new Vector<HspAttributeDimension>();
                    }
                    attrDimsForDim.addElement(aDim);
                }
            }
        }
        return attrDimsForDim;
    }

    public void validateAttributeDimensionMember(int attrType, HspMember hspMbr, HashMap errors) {
        if (hspMbr != null) {
            try {
                //Validate attribute member name
                validateAttributeName(hspMbr.getName());
            } catch (Throwable x) {
                if (errors != null)
                    errors.put(hspMbr, x);
            }
            // Find duplicate
            if (attrType != HspConstants.kDataAttributeBoolean) {
                HspAttributeDimension attrDim = getAttributeDimension(hspMbr.getDimId());
                // only perform duplicate check on indexed attribute dims
                if (attrDim != null && attrDim.isIndexed()) {
                    HspMember anyMbr = getMemberByName(hspMbr.getName());
                    if (anyMbr != null && anyMbr.getId() != hspMbr.getId()) {
                        Properties props = new Properties();
                        props.put("OBJECT_NAME", hspMbr.getName());
                        errors.put(hspMbr, new HspRuntimeException("MSG_SQL_DUPLICATE_OBJECT", props));
                    }
                }
            }
            if (hspMbr.hasChildren()) {
                validateAttributeName(hspMbr.getName());
                Vector<HspMember> children = getChildMembers(hspMbr.getDimId(), hspMbr.getId(), 0);
                if (children != null) {
                    if (attrType == HspConstants.kDataAttributeBoolean) {
                        if (children.size() != 2) {
                            errors.put(hspMbr, new HspRuntimeException("INVALID_MEMBERS_BOOLEAN_ATTR_DIM"));
                        } else {
                            for (Iterator<HspMember> childItemIt = children.iterator(); childItemIt.hasNext(); ) {
                                HspMember childMbr = childItemIt.next();

                                if ((childMbr != null)) {
                                    try {
                                        HspUtils.validateBoolean(childMbr.getName());
                                    } catch (Throwable x) {
                                        if (errors != null)
                                            errors.put(childMbr, x);
                                    }
                                }
                            }
                        }
                    } else {
                        for (Iterator<HspMember> it = children.iterator(); it.hasNext(); )
                            validateAttributeDimensionMember(attrType, it.next(), errors);
                    }
                }
            } else {
                switch (attrType) {
                case HspConstants.kDataAttributeDate:
                    try {
                        validateDateString(hspMbr.getName());
                    } catch (Throwable x) {
                        errors.put(hspMbr, x);
                    }
                    break;
                case HspConstants.kDataAttributeNumeric:
                    try {
                        HspUtils.validateNumericString(hspMbr.getName());
                    } catch (Throwable x) {
                        errors.put(hspMbr, x);
                    }
                    break;
                }
            }
        }
    }

    /**
     * Validates an attribute dimension. Puts errors into the errors hashmap
     * @param attrDim
     * @param errors
     */
    public void validateAttributeDimension(HspAttributeDimension attrDim, HashMap errors) {
        if (attrDim == null)
            throw new IllegalArgumentException("Invalid parameters for validateAttributeMember");


        else {
            Vector<HspMember> children = getChildMembers(attrDim.getId(), attrDim.getId(), 0);
            if (children == null || children.size() == 0) {
                // Text attribute dimensions without any child members are supported by essbase and
                // previous release of planning.
                if (attrDim.getAttributeType() != HspConstants.kDataAttributeText)
                    errors.put(attrDim, new HspRuntimeException("ATTRIBUTE_DIMENSION_WITHOUT_MEMBERS"));
            } else {
                int attrType = attrDim.getAttributeType();
                for (Iterator<HspMember> it = children.iterator(); it.hasNext(); )
                    validateAttributeDimensionMember(attrType, it.next(), errors);
            }
        }
    }

    /**
     * Validates all attribute dimensions for base dimension. This is needed because,
     * essbase supports numeric, date type attribute dimensions with string members as non-level zero members
     * Since the UI currently allows creation/modifcation one member at a time, a validation type
     * api is needed.
     * @param baseDimId
     * @return HashMap of errors list. HspMember objects are the keys and HspRuntimeExceptions are the values
     */
    public HashMap validateAttributeMembersForBaseDim(int baseDimId) {
        Vector<HspAttributeDimension> attrDims = getAttributeDimensionsForBaseDim(baseDimId);
        if (attrDims != null) {
            HashMap errors = new HashMap();
            for (Iterator<HspAttributeDimension> it = attrDims.iterator(); it.hasNext(); ) {
                HspAttributeDimension attrDim = it.next();
                validateAttributeDimension(attrDim, errors);
            }
            return errors;
        }
        return null;
    }

    public HashMap validateAttributeMembersForBaseDim(String dimName) {
        HspDimension dim = getDimRoot(dimName);
        if (dim != null)
            return validateAttributeMembersForBaseDim(dim.getId());
        return null;
    }

    public Vector<HspAttributeDimension> getAttributeDimensionsForBaseDim(int dimId, int planType) {
        Vector<HspAttributeDimension> attributeDimensions = attributeDimensionCache.getUnfilteredCache();
        Vector<HspAttributeDimension> attrDimsForDim = null;
        HspAttributeDimension aDim = null;
        if (attributeDimensions != null) {
            for (int i = 0; i < attributeDimensions.size(); i++) {
                aDim = attributeDimensions.elementAt(i);
                if ((aDim.baseDimId == dimId) && ((planType & aDim.getUsedIn()) != 0)) {
                    if (attrDimsForDim == null) {
                        attrDimsForDim = new Vector<HspAttributeDimension>();
                    }
                    attrDimsForDim.addElement(aDim);
                }
            }
        }
        return attrDimsForDim;
    }

    /**
     * {@inheritDoc}
     *
     * @param sessionId {@inheritDoc}
     * @return {@inheritDoc}
     */
    public Vector<HspReplacementDimension> getReplacementDimensions(int sessionId) {
        hspStateMgr.verify(sessionId);
        return replacementDimensionCache.getUnfilteredCache();
    }

    /**
     * {@inheritDoc}
     *
     * @param dimId {@inheritDoc}
     * @return {@inheritDoc}
     */
    public HspReplacementDimension getReplacementDimension(int dimId) {
        return replacementDimensionCache.getObject(dimId);
    }

    /**
     * {@inheritDoc}
     *
     * @param dimName {@inheritDoc}
     * @return {@inheritDoc}
     */
    public HspReplacementDimension getReplacementDimension(String dimName) {
        return replacementDimensionCache.getObject(dimName);
    }

    /**
     * {@inheritDoc}
     *
     * @param planType {@inheritDoc}
     * @param sessionId {@inheritDoc}
     * @return {@inheritDoc}
     */
    public Vector<HspReplacementDimension> getReplacementDimensions(int planType, int sessionId) {
        Vector<HspReplacementDimension> allReplacementDimensions = getReplacementDimensions(sessionId);
        if (allReplacementDimensions == null || allReplacementDimensions.isEmpty())
            return allReplacementDimensions;
        Vector<HspReplacementDimension> replacementDimensions = new Vector<HspReplacementDimension>(allReplacementDimensions.size());
        for (HspReplacementDimension replacementDimension : allReplacementDimensions) {
            //If it is used in at least one of the plan types we care about, then we add it to the list.
            if (replacementDimension != null && ((replacementDimension.getUsedIn() & planType) > 0))
                replacementDimensions.add(replacementDimension);
        }
        return replacementDimensions;
    }

    /**
     * {@inheritDoc}
     *
     * @param planType {@inheritDoc}
     * @param sessionId {@inheritDoc}
     * @return {@inheritDoc}
     */
    public Vector<HspReplacementDimension> getDefaultReplacementDimensions(int planType, int sessionId) {
        return new Vector<HspReplacementDimension>(0);
    }

    /**
     * {@inheritDoc}
     *
     * @param member {@inheritDoc}
     * @return {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws HspRuntimeException {@inheritDoc}
     */
    public List<HspMember> getReplacedMembers(HspReplacementMember member) {
        HspUtils.verifyArgumentNotNull(member, "member");
        HspReplacementDimension replacementDim = (HspReplacementDimension)getDimRoot(member.getDimId());
        if (replacementDim == null)
            throw new InvalidDimensionException(String.valueOf(member.getDimId()));

        List<HspMember> members = new ArrayList<HspMember>(4);
        int[] dimIds = replacementDim.getDimIds();
        int numDimIds = dimIds == null ? 0 : dimIds.length;
        for (int i = 0; i < numDimIds; i++) {
            int dimId = dimIds[i];
            int memberId = member.getMemberId(i);
            HspMember replacedMember = getDimMember(dimId, memberId);
            if (replacedMember == null)
                throw new HspRuntimeException("MSG_ERR_INVALID_REPLACEMENT_MEMBER_REFERENCE", HspUtils.createProps("MEMBER_NAME", member.getMemberName()));
            members.add(replacedMember);
        }
        return members;
    }

    public Vector<HspPMDimension> getPMDimensions(int sessionId) {
        hspStateMgr.verify(sessionId);
        return pmDimensionCache.getUnfilteredCache();
    }

    /*     public Vector<HspDPDimension> getDPDimensions(int sessionId) {
        hspStateMgr.verify(sessionId);
        return dpDimensionCache.getUnfilteredCache();
    } */

    //  public Vector<HspDPDimension> getDimensionsByType(int dimType, int sessionId) {
    //      hspStateMgr.verify(sessionId);
    //      return dpDimensionCache.getObjects(dimTypeKeyDef, dimTypeKeyDef.createKeyFromDimType(dimType));
    //  }

    //  public Vector<HspDPDimension> getDPDimensions(int dpTypeId, int sessionId) {
    //      hspStateMgr.verify(sessionId);
    //      return dpDimensionCache.getObjects(dpDimKeyDef, dpDimKeyDef.createKeyFromDPMemberId(dpTypeId));
    //  }

    //TODO: Currently this method returns vector of all DP Dimensions if scenarioId =0 and a vector of 1 if any scenarioId is given.
    //Separate the methods for both cases.

    public Vector<HspDPDimension> getDPDimensions(int scenarioId, int sessionId) {
        hspStateMgr.verify(sessionId);
        if (scenarioId != 0)
            return dpDimensionCache.getObjects(dpDimScenarioKeyDef, dpDimScenarioKeyDef.createKeyFromScenarioId(scenarioId));
        else
            return dpDimensionCache.getUnfilteredCache();
    }

    public HspDPDimension getDPDimension(int scenarioId, int versionId) {
        //hspStateMgr.verify(sessionId);
        HspDPDimension dpDimension = null;
        Vector<HspDPDimension> dimensions = dpDimensionCache.getUnfilteredCache();
        for (HspDPDimension dim : dimensions) {
            if (dim.getObjectName().equalsIgnoreCase("DP_" + scenarioId + "_" + versionId)) {
                dpDimension = dim;
                break;
            }
        }
        return dpDimension;
        //      if(scenarioId != 0)
        //          return dpDimensionCache.getObjects(dpDimScenarioKeyDef, dpDimScenarioKeyDef.createKeyFromScenarioId(scenarioId));
        //      else
        //          return dpDimensionCache.getUnfilteredCache();
    }

    public HspDPDimension getDPDimension(int dpDimensionId) {
        HspDPDimension dim = dpDimensionCache.getObject(dpDimensionId);
        return dim;
    }

    public HspDPDimension getDPDimension(String dpDimensionName) {
        HspDPDimension dim = dpDimensionCache.getObject(dpDimensionName);
        return dim;
    }

    public HspDPMember getDPMember(int dpDimensionId, int dpMemberId, int sessionId) {
        Vector<HspDPMember> dpMembers = getDimMembers(dpDimensionId, false, sessionId);
        for (HspDPMember member : dpMembers) {
            if (member.getId() == dpMemberId) {
                return member;
            }
        }
        return null;
    }

    public void deleteDPDimension(int dpDimensionId, int sessionId) throws Exception {
        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        HspDPDimensionAction action = new HspDPDimensionAction();
        HspDPDimension dpDimension = getDPDimension(dpDimensionId);
        actionSet.addAction(action, HspActionSet.DELETE, dpDimension);
        actionSet.doActions();
    }

    public List<HspDPMember> getDecisionPackages(int sessionId) {
        hspStateMgr.verify(sessionId);
        List<HspDPMember> dps = new ArrayList<HspDPMember>();
        Vector<HspDPDimension> dpDims = dpDimensionCache.getUnfilteredCache();

        for (HspDPDimension dpDim : dpDims) {
            Vector<HspDPMember> children = getDimMember(dpDim.getId(), dpDim.getObjectName()).getChildren();
            if (children != null && !children.isEmpty()) {
                dps.addAll(children);
            }
        }

        return dps;
    }

    public List<HspDPMember> getDecisionPackages(String scenarioName, String versionName, int sessionId) {
        hspStateMgr.verify(sessionId);
        int scenarioId = getScenario(scenarioName).getId();
        int versionId = getVersion(versionName).getId();
        HspDPDimension dpDim = getDPDimension(scenarioId, versionId); //, sessionId);
        return dpDim != null ? getDimMember(dpDim.getId(), dpDim.getObjectName()).getChildren() : new ArrayList<HspDPMember>();
    }

    public List<HspDPMember> getBudgetRequests(String scenarioName, String versionName, String decisionPackageName, int sessionId) {
        hspStateMgr.verify(sessionId);
        int scenarioId = getScenario(scenarioName).getId();
        int versionId = getVersion(versionName).getId();
        HspDPDimension dpDim = getDPDimension(scenarioId, versionId); //, sessionId);
        HspDPMember dpMember = (HspDPMember)getDimMember(dpDim.getId(), decisionPackageName);
        return dpMember != null ? dpMember.getChildren() : null;
    }

    private HspDPMember getBudgetRequestMember(HspFormCell scenarioCell, HspFormCell versionCell, HspFormCell brCell) {
        if (scenarioCell == null || versionCell == null || brCell == null)
            return null;
        int brDimMemberId = (brCell != null ? brCell.getMbrId() : 0);
        HspDPSVBRBinding brSceVer = brDimMemberId == 0 ? null : getBRScenarioVersionInfo(brDimMemberId, getBaseMemberId(scenarioCell), getBaseMemberId(versionCell));

        int dpBRMemberId = brSceVer != null ? brSceVer.getDpMemberId() : 0;
        return dpBRMemberId > 0 ? (HspDPMember)hspPMDB.getDPMember(getBaseMemberId(scenarioCell), getBaseMemberId(versionCell), dpBRMemberId) : null;
    }

    public HspDPMember getDPMember(HspFormCell scenarioCell, HspFormCell versionCell, HspFormCell brCell) {
        HspDPMember dpBRMember = getBudgetRequestMember(scenarioCell, versionCell, brCell);
        return dpBRMember != null ? (HspDPMember)getDimMember(dpBRMember.getDimId(), dpBRMember.getParentId()) : null;
    }

    public List<HspMember> getEntityMembersReferencedByBudgetRequest(HspFormMember scenarioCell, HspFormMember versionCell, HspFormMember brCell) {
        List<HspMember> entityList = new ArrayList<HspMember>();
        HspDPMember dpBRMember = getBudgetRequestMember(new HspFormCell(scenarioCell), new HspFormCell(versionCell), new HspFormCell(brCell));
        if (dpBRMember != null) {
            if (dpBRMember.getEntityId() > 0) {
                HspMember entity = getDimMember(HspConstants.kDimensionEntity, dpBRMember.getEntityId());
                if (entity != null)
                    entityList.add(entity);
            }
            if (dpBRMember.getSharingEntityIds() != null && dpBRMember.getSharingEntityIds().length > 0) {
                for (int entityId : dpBRMember.getSharingEntityIds()) {
                    HspMember entity = getDimMember(HspConstants.kDimensionEntity, entityId);
                    if (entity != null)
                        entityList.add(entity);
                }
            }
        }

        return entityList;
    }

    private int getBaseMemberId(HspFormMember formCell) {
        return formCell.isSharedMember() ? formCell.getBaseMemberId() : formCell.getMbrId();
    }

    public List<HspDPMember> getIncludedBudgetRequest(String scenarioName, String versionName, int sessionId) {
        hspStateMgr.verify(sessionId);
        List<HspDPMember> includedBRMembers = new ArrayList<HspDPMember>();
        int scenarioId = getScenario(scenarioName).getId();
        int versionId = getVersion(versionName).getId();
        HspDPDimension dpDim = getDPDimension(scenarioId, versionId); //, sessionId);
        if (dpDim == null)
            return includedBRMembers;
        List<HspDPMember> dpMembers = getDimMember(dpDim.getId(), dpDim.getObjectName()).getChildren();
        if (dpMembers != null) {
            for (HspDPMember dpMember : dpMembers) {
                if (dpMember.getBudgetImpact()) {
                    List<HspDPMember> brMembers = dpMember.getChildren();
                    if (brMembers != null) {
                        for (HspDPMember brMember : brMembers) {
                            if (brMember.getBudgetImpact() && brMember.getBrDimMemberId() > 0) {
                                includedBRMembers.add(brMember);
                            }
                        }
                    }
                }
            }
        }

        return includedBRMembers;
    }

    public String getDPMemberJustification(int dpMemberId) throws Exception {
        String justification = "";
        Connection connection = null;
        try {
            String[] params = new String[] { dpMemberId + "" };
            connection = hspSQL.getConnection();
            Vector<HspDPMember> items = hspSQL.executeQuery("SQL_GET_DP_MEMBER_JUSTIFICATION", params, connection, HspDPMember.class);
            if ((items != null) && (items.size() > 0)) {
                justification = items.firstElement().getJustification();
            }
        } finally {
            if (connection != null)
                hspSQL.releaseConnection(connection);
        }
        return justification;
    }

    public HspAttributeDimension getAttributeDimension(int dimId) {
        return attributeDimensionCache.getObject(dimId);
    }

    public HspAttributeDimension getAttributeDimension(String dimensionName) {
        return attributeDimensionCache.getObject(dimensionName);
    }

    public int getNumDimMembers(int dimId) {
        return getMembersCache(dimId).getNumElements();
    }

    public <MBR extends HspMember> Vector<MBR> getDimMembers(int dimId, boolean filter, int sessionId) {
        return getDimMembers(dimId, filter, false, sessionId);
    }

    private <MBR extends HspMember> Vector<MBR> getDimMembers(int dimId, boolean filter, boolean includeDTSMembers, int sessionId) {
        hspStateMgr.verify(sessionId);
        if (filter && (isDimensionSecured(dimId, sessionId))) {
            //noinspection unchecked
            Vector<MBR> members = (Vector<MBR>)getMembersCache(dimId).getFilteredCache(hspStateMgr.getUserId(sessionId), HspConstants.ACCESS_READ | HspConstants.ACCESS_WRITE);

            if (dimId != HspConstants.kDimensionTimePeriod) {
                return members;
            } else {
                Vector<MBR> filtMembers = new Vector<MBR>();
                for (MBR mbr : members) {
                    HspTimePeriod tp = (HspTimePeriod)mbr;
                    if (tp.getType() != HspConstants.DTS_TP_TYPE || includeDTSMembers) {
                        filtMembers.add(mbr);
                    }
                }
                return filtMembers;
            }

            //} else {

            //return getMembersCache(dimId).getUnfilteredCache();
            //}
        } else {

            if (dimId == HspConstants.kDimensionRates) {
                //noinspection unchecked
                return (Vector<MBR>)getHspRatesMembers(getMembersCache(dimId).getUnfilteredCache());
            } else if (dimId == HspConstants.kDimensionTimePeriod && !includeDTSMembers) {
                // always filter out DTS members as you would not want to see them as regular members
                // except when we actually need them to process usedIn propagation
                return getDimMembersWithFilter(HspConstants.kDimensionTimePeriod, HspConstants.DTS_TP_TYPE, false, sessionId);
            } else {
                //noinspection unchecked
                return (Vector<MBR>)getMembersCache(dimId).getUnfilteredCache();
            }
        }
    }

    public HspObjectNote getObjectNote(int noteId, int objectId, int sessionId) {
        hspStateMgr.verify(sessionId);
        HspUpdateableCache<HspObjectNote> dpNotesCache = objectNotesCache.getSubCache(objectNoteMPCKeyGenerator.generateKey(objectId));
        return dpNotesCache != null ? dpNotesCache.getObject(noteId) : null;
    }

    public Vector<HspObjectNote> getNotesByType(int noteType, int objectId, int sessionId) {
        hspStateMgr.verify(sessionId);
        HspUpdateableCache<HspObjectNote> dpNotesCache = objectNotesCache.getSubCache(objectNoteMPCKeyGenerator.generateKey(objectId));
        return dpNotesCache != null ? dpNotesCache.getObjects(objNoteTypeKeyDef, objNoteTypeKeyDef.createKeyFromType(noteType)) : null;
    }

    public Vector<HspObjectNote> getObjectNotes(int objectId, int sessionId) {
        hspStateMgr.verify(sessionId);
        return objectNotesCache.getUnfilteredCache(objectNoteMPCKeyGenerator.generateKey(objectId));
    }

    public void saveObjectNote(HspObjectNote note, int sessionId) throws Exception {
        hspStateMgr.verify(sessionId);
        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        HspObjectNoteAction action = new HspObjectNoteAction();

        if (note.getNoteId() == -1) {
            actionSet.addAction(action, HspActionSet.ADD, note);
        } else {
            actionSet.addAction(action, HspActionSet.UPDATE, note);
        }
        actionSet.doActions();
    }

    public void deleteObjectNote(HspObjectNote note, int sessionId) throws Exception {
        hspStateMgr.verify(sessionId);
        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        HspObjectNoteAction action = new HspObjectNoteAction();
        actionSet.addAction(action, HspActionSet.DELETE, note);
        actionSet.doActions();
    }

    //Hsp_Rates members should be made available on Member Selector page also

    private Vector<HspMember> getHspRatesMembers(Vector<HspMember> hiddenConstantMembers) {
        Vector<HspMember> hiddenMembers = new Vector<HspMember>();
        Vector<HspCurrency> allCurrencies = hspCurDB.getSelectedCurrencies(internalSessionId);
        for (int i = 0; i < hiddenConstantMembers.size(); i++) {
            hiddenMembers.insertElementAt(hiddenConstantMembers.get(i), i);
        }
        for (int i = 0; i < allCurrencies.size(); i++) {
            HspCurrency currency = allCurrencies.get(i);
            HspMember tempMember = new HspMember();
            tempMember.setDimId(HspConstants.kDimensionRates);
            tempMember.setObjectId(HspConstants.MEMBER_ID_HSP_INPUT_CURRENCY + (i + 1));
            tempMember.setObjectName("HSP_Rate_" + currency.getCurrencyCode());
            tempMember.setObjectType(HspConstants.gObjType_UserDefinedMember);
            tempMember.setParentId(HspConstants.kDimensionRates);
            tempMember.setUsedIn(HspConstants.PLAN_TYPE_ALL);
            tempMember.setPosition(50020 + (i * 10));
            tempMember.makeReadOnly();
            hiddenMembers.insertElementAt(tempMember, hiddenMembers.size());
        }
        return hiddenMembers;

    }

    public <MBR extends HspMember> Vector<MBR> getDimMembersWithFilter(int dimId, int filterValue, boolean shouldMatch, int sessionId) {
        hspStateMgr.verify(sessionId);
        //noinspection unchecked
        return (Vector<MBR>)getMembersCache(dimId).getFilteredCache(filterValue, shouldMatch);
    }

    public synchronized int getDimIndex(int dimId) {
        for (int loop1 = 0; loop1 < orderedDimIds.length; loop1++) {
            if (orderedDimIds[loop1] == dimId)
                return loop1;
        }
        throw new IllegalArgumentException("getDimIndex(" + dimId + "): The specified dimId is not in the dim cache.");


    }

    public synchronized int[] getOrderedDimIds() {
        return HspUtils.cloneArray(orderedDimIds);
    }

    public synchronized int getDimIdByIndex(int index) {
        if (orderedDimIds.length >= index && index > -1)
            return orderedDimIds[index];
        throw new IllegalArgumentException("getDimIdByIndex(" + index + "): The specified index is not in the dim cache.");


    }

    public synchronized int[] getIndexedDimIds() {
        if (orderedDimIds == null)
            return null;

        int[] result = new int[orderedDimIds.length];
        System.arraycopy(orderedDimIds, 0, result, 0, orderedDimIds.length);

        return result;
    }

    /**
     * This additionally looks for dimId in the reportingCubeDims Map as dim is unique by Id but
     * reportingCubeDim name can be duplicate with any existing dimension names.
     */
    public HspDimension getDimRoot(int dimId) {
        HspDimension dimension = dimensionCache.getObject(dimId);
        if (dimension == null)
            dimension = hiddenDimensionsCache.getObject(dimId);
        if (dimension == null)
            dimension = metricDimensionCache.getObject(dimId);
        if (dimension == null)
            dimension = hspRepDimMbrCacheMgr.getReportingCubeDimension(dimId);
        if (dimension == null)
            dimension = replacementDimensionCache.getObject(dimId);
        if (dimension == null)
            dimension = pmDimensionCache.getObject(dimId);
        if (dimension == null)
            dimension = dpDimensionCache.getObject(dimId);
        if (dimension == null)
            dimension = virtualDimensionsCache.getObject(dimId);

        //        if (dimension == null)
        //            dimension = brDimensionCache.getObject(dimId);

        return dimension;
    }

    public HspDimension getDimRoot(String dimName) {
        HspDimension dimension = dimensionCache.getObject(dimName);
        if (dimension == null)
            dimension = hiddenDimensionsCache.getObject(dimName);
        if (dimension == null)
            dimension = metricDimensionCache.getObject(dimName);
        if (dimension == null)
            dimension = replacementDimensionCache.getObject(dimName);
        if (dimension == null)
            dimension = pmDimensionCache.getObject(dimName);
        if (dimension == null)
            dimension = dpDimensionCache.getObject(dimName);
        if (dimension == null)
            dimension = virtualDimensionsCache.getObject(dimName);
        //        if (dimension == null)
        //            dimension = brDimensionCache.getObject(dimName);

        // ASARAF - do not return the reportingCube Dimension by name as by name they are not unique.
        // They are unique only for a given essbase server/app/cube combination.
        return dimension;
    }

    public HspDimension getDimRoot(int planTypes, int dimId) {
        return getDimRoot(planTypes, dimId, null);
    }

    public HspDimension getDimRoot(int planTypes, String dimName) {
        return getDimRoot(planTypes, -1, dimName);
    }

    public HspDimension getDimRoot(int planTypes, int dimId, String dimName) {
        HspDimension dimension = null;

        Vector<? extends HspDimension> allDimensions = dimensionCache.getUnfilteredCache();
        dimension = getDimRoot(allDimensions, planTypes, dimId, dimName);
        if (dimension == null) { // Check for metric dimensions
            allDimensions = metricDimensionCache.getUnfilteredCache();
            dimension = getDimRoot(allDimensions, planTypes, dimId, dimName);
        }
        if (dimension == null) { // Check for replacement dimensions
            allDimensions = replacementDimensionCache.getUnfilteredCache();
            dimension = getDimRoot(allDimensions, planTypes, dimId, dimName);
        }

        if (dimension == null) { // Check for external reporting cube
            HspCube essCube = getEssbaseCubeByPlanType(planTypes);
            if (essCube != null) {
                allDimensions = getReportingCubeDimensions(getEssbaseServer(essCube.getEssbaseServerId(), internalSessionId), essCube.getAppName(), essCube.getCubeName(), internalSessionId);
                dimension = getDimRoot(allDimensions, planTypes, dimId, dimName);
            }
        }

        return dimension;
    }

    private HspDimension getDimRoot(Vector<? extends HspDimension> allDimensions, int planTypes, int dimId, String dimName) {
        if (allDimensions == null || allDimensions.size() <= 0)
            return null;
        HspDimension dimension = null;
        for (int loop1 = 0; loop1 < allDimensions.size(); loop1++) {
            HspDimension dim = allDimensions.elementAt(loop1);
            //If the dimention is not null, and is not an Attribute dimention, then it is a base dimention
            //If it is used in at least one of the plan types we care about, and thename matches,
            // then we return that dimension.
            if ((dim != null) && (dim.getDimType() != HspConstants.kDimTypeAttribute) && (((dimId != -1) && (dim.getId() == dimId)) || ((dimName != null) && (dim.getName().equalsIgnoreCase(dimName)))) && ((dim.getUsedIn() & planTypes) > 0)) {
                dimension = dim;
                break;
            }
        }
        return dimension;
    }

    public List<HspMemberOnFlyDetail> getMemberOnFlyDetailsForDim(int dimId) {
        return HspUtils.createFilteredList(mbrOnFlyDetailCache.getUnfilteredCache(), new HspMemberDynamicChildEnabledFilter(this, dimId));
    }

    public HspDimension getDimRootByPsId(int psId) {
        return dimensionCache.getObject(memberPsIdKeyDef, memberPsIdKeyDef.createKeyFromPsId(psId));
    }

    private HspMember fillMemberPTProps(HspMember member) {
        if (member != null) {
            Vector<HspMemberPTProps> props = mbrPTPropsCache.getObjects(mbrPTPropsMemberIdKeyDef, mbrPTPropsMemberIdKeyDef.createKeyFromId(member.getId()));
            if (member.isReadOnly())
                member = (HspMember)member.cloneForUpdate();
            member.setMemberPTPropsToBeSaved(props);
            member = fillPTLockOpts(member);
            member.clearClonedFrom();
            member.makeReadOnly();
        }
        return member;
    }

    private HspMember fillPTLockOpts(HspMember member) {
        if (member != null) {
            Vector<HspCube> props = cubeCache.getUnfilteredCache();
            Map<Integer, Integer> ptLockOptsMap = new HashMap<Integer, Integer>();
            for (HspCube cube : props) {
                ptLockOptsMap.put(cube.getPlanType(), cube.getPtLockOpts());
            }
            member.setPlanTypeLockOpts(ptLockOptsMap);
        }
        return member;
    }

    public HspMember getDimMember(int dimId, int mbrId) {
        //Should never return the Dim Root becasue the type is not compatible with the other members
        //		if (dimId != mbrId)
        return fillMemberPTProps(getDimMemberWithoutPtProps(dimId, mbrId));

        //		else
        //			return getDimRoot(dimId);
    }

    private HspMember getDimMemberWithoutPtProps(int dimId, int mbrId) {
        return getMembersCache(dimId).getObject(mbrId);
    }

    private HspMember getActionSetMemberOrInput(HspMember member, HspActionSet actionSet) {
        if (actionSet != null && member != null) {
            CachedObject pendingUpdate = actionSet.getCachedObject(member);
            if (pendingUpdate != null && member.getClass().equals(pendingUpdate.getClass()))
                member = (HspMember)pendingUpdate;
        }
        return member;
    }

    private HspMember getDimMember(int dimId, int mbrId, HspActionSet actionSet) {
        HspMember member = getDimMember(dimId, mbrId);
        return getActionSetMemberOrInput(member, actionSet);
    }

    private HspMember getDimMember(int dimId, String mbrName, HspActionSet actionSet) {
        HspMember member = getDimMember(dimId, mbrName);
        return getActionSetMemberOrInput(member, actionSet);
    }

    public <MBR extends HspMember> MBR getDimBaseMember(int dimId, int mbrId) {
        HspMember member = getDimMember(dimId, mbrId);
        if (member != null && member.isSharedMember())
            member = getDimMember(dimId, member.getBaseMemberId());
        //noinspection unchecked
        return (MBR)fillMemberPTProps(member);
    }

    public HspMember getDimMember(int dimId, CachedObjectKeyDef keyDef, Object key) {
        //Should never return the Dim Root becasue the type is not compatible with the other members
        //		if (dimId != mbrId)
        return fillMemberPTProps(getMembersCache(dimId).getObject(keyDef, key));
        //		else
        //			return getDimRoot(dimId);
    }

    public HspMember getDimMember(int dimId, CachedObjectKeyDef keyDef, Object key, boolean filter, int sessionId) {
        HspMember member = getMembersCache(dimId).getObject(keyDef, key);
        if (!filter || hasAccess(dimId, member, sessionId))
            return fillMemberPTProps(member);
        return null;
    }

    private boolean hasAccess(int dimId, HspMember member, int sessionId) {
        boolean hasAccess = true;
        if (member != null) {
            if ((isDimensionSecured(dimId, sessionId))) {
                if (member.getId() == member.getDimId()) {
                    hasAccess = true;
                } else {
                    hasAccess = ((member != null) && (((HspConstants.ACCESS_READ | HspConstants.ACCESS_WRITE) & hspSecDB.getAccess(hspStateMgr.getUserId(sessionId), member)) > 0));
                }
            }
        }
        return hasAccess;
    }

    public HspMember getDimMember(int dimId, String mbrName, boolean filter, int sessionId) {
        if (filter)
            hspStateMgr.verify(sessionId);
        if (mbrName == null) {
            return null;
        } else {
            HspMember member = getMembersCache(dimId).getObject(mbrName);
            if (member == null) {
                member = getDimMemberByUniqueName(dimId, mbrName);
            }

            boolean isValid = true;
            if (member != null) {
                if (filter && (isDimensionSecured(dimId, sessionId))) {
                    if (member.getId() == member.getDimId()) {
                        isValid = true;
                    } else {
                        isValid = ((member != null) && (((HspConstants.ACCESS_READ | HspConstants.ACCESS_WRITE) & hspSecDB.getAccess(hspStateMgr.getUserId(sessionId), member)) > 0));
                    }
                }
            }

            if (isValid) {
                return fillMemberPTProps(member);
            } else {
                return (null);
            }
        }
    }

    public String[] splitMemberName(String uniqueName) {
        HspUtils.verifyStringArgumentNullOrEmpty(uniqueName, "uniqueName");
        // Assume name is [parentName].[childName]
        String[] names = uniqueName.split("\\]\\.\\[");
        if (names.length == 2) {
            String name0 = names[0].substring(1);
            String name1 = names[1].substring(0, names[1].length() - 1);
            names[0] = name0;
            names[1] = name1;
        }
        return names;
    }

    private void setUniqueName(HspMember member, HspActionSet actionSet) throws Exception {
        member.setUniqueName(getUniqueName(member, actionSet));
    }

    public String getUniqueName(HspMember member) {
        try {
            return getUniqueName(member, null);
        } catch (Exception e) {
            throw HspUtils.toRuntimeException(e);
        }
    }

    private String getUniqueName(HspMember member, HspActionSet actionSet) throws Exception {
        // Only qualify base members if cfg.isBaseWithSharedQualified is true
        if (member == null || (!member.isSharedMember() && !hspJS.getSystemCfg().isBaseWithSharedQualified()))
            return null;
        List<HspMember> shareds = member.isSharedMember() ? Collections.<HspMember>emptyList() : getSharedMembersOfBase(member.getDimId(), member.getId(), actionSet, internalSessionId);
        if (member.isSharedMember() || HspUtils.size(shareds) > 0) {
            HspMember parent = getDimMember(member.getDimId(), member.getParentId(), actionSet);
            if (parent == null)
                parent = (HspMember)member.getParent();
            if (parent == null)
                return null;

            StringBuilder sb = new StringBuilder();
            sb.append("[");
            sb.append(parent.getMemberName());
            sb.append("].[");
            sb.append(member.getMemberName());
            sb.append("]");
            return sb.toString();
        }
        return null;
    }

    private HspMember getDimMemberByUniqueName(int dimId, String uniqueName) {
        HspMember member = null;

        if (uniqueName != null) {
            if (uniqueName.startsWith("[") && uniqueName.endsWith("]")) {
                String[] names = splitMemberName(uniqueName);

                if (names.length == 2) {
                    HspMember parentMbr = getMembersCache(dimId).getObject(names[0]);
                    HspMember childMbr = getMembersCache(dimId).getObject(names[1]);

                    if (childMbr != null && parentMbr != null) {
                        // check if parent matches parentMbr
                        if (childMbr.getParentId() == parentMbr.getId()) {
                            member = childMbr;
                        } else {
                            // try looking up member as a shared member
                            member = getDimSharedMember(dimId, names[1], names[0], internalSessionId);
                        }
                    }
                }
            } else {
                member = getMembersCache(dimId).getObject(uniqueName);
            }
        }
        return member;
    }

    public HspMember getDimMember(int dimId, String mbrName) {
        return getDimMember(dimId, mbrName, false, internalSessionId);
    }

    public HspMember getMemberIdAllowMbrNameMatch(int dimId, String aliasName, int aliasTableId) {
        if (hspAlsDB != null) {
            int memberId = hspAlsDB.getMemberIdAllowMbrNameMatch(aliasTableId, dimId, aliasName);
            if (memberId > 0) {
                return getDimMember(dimId, memberId);
            }
        }
        return null;
    }

    public HspMember getDimMemberFromAlias(int dimId, String aliasName, int aliasTableId) {
        if (hspAlsDB != null) {
            int memberId = hspAlsDB.getMemberId(aliasTableId, dimId, aliasName);
            if (memberId > 0) {
                return getDimMember(dimId, memberId);
            }
        }
        return null;
    }

    public HspMember getDimMemberByPsId(int dimId, int psId) {
        return getMembersCache(dimId).getObject(memberPsIdKeyDef, memberPsIdKeyDef.createKeyFromPsId(psId));
    }

    public Vector<HspAccount> getAccountsWithSubAccountType(int subAccountType, int sessionId) {
        hspStateMgr.verify(sessionId);
        return accountCache.getObjects(accountSubAccountKeyDef, accountSubAccountKeyDef.createKeyFromSubAccountType(subAccountType));
    }

    public Vector<HspAccount> getAccountsWithSubAccountType(int subAccountType, int accessFlags, int sessionId) {
        int userId = hspStateMgr.getUserId(sessionId);
        return accountCache.getFilteredObjects(accountSubAccountKeyDef, accountSubAccountKeyDef.createKeyFromSubAccountType(subAccountType), userId, accessFlags);
    }

    public HspEntity getEntityWithRequisitionNumber(String requisitionNumber) {
        return (HspEntity)getMembersCache(HspConstants.kDimensionEntity).getObject(entityRequisitionNumberKeyDef, entityRequisitionNumberKeyDef.createKeyFormRequisitionNumber(requisitionNumber));
    }

    public int getPositionInDimension(int dimId, int memberId) {
        GenericCache<HspMember> cache = getMembersCache(dimId);
        int position = cache.getPositionInTree(CachedObjectIdKeyDef.CACHED_OBJECT_ID_KEY.createKeyFromId(memberId));
        return position;
    }

    public int getPositionInDimension(HspMember member) {
        int position = -1;
        if (member != null) {
            GenericCache<HspMember> cache = getMembersCache(member.getDimId());
            position = cache.getPositionInTree(CachedObjectIdKeyDef.CACHED_OBJECT_ID_KEY.createKey(member));
        }
        return position;

    }

    public HspMember getMemberAtPosition(int dimId, int position) {
        GenericCache<HspMember> cache = getMembersCache(dimId);
        return (HspMember)cache.getObjectAtPositionInTree(position);
    }

    public HspMember getAttributeMember(int baseDimId, int mbrId) {
        Vector<HspAttributeDimension> attributeDimensionsForBase = getAttributeDimensionsForBaseDim(baseDimId);
        HspMember member = null;
        HspAttributeDimension dim = null;
        if (attributeDimensionsForBase != null) {
            for (int d = 0; d < attributeDimensionsForBase.size(); d++) {
                dim = attributeDimensionsForBase.elementAt(d);
                member = getDimMember(dim.getId(), mbrId);
                if (member != null) {
                    return (member);
                }
            }
        }
        return (member);
    }

    public HspMember getAttributeMember(int baseDimId, String memberName) {
        Vector<HspAttributeDimension> attributeDimensionsForBase = getAttributeDimensionsForBaseDim(baseDimId);
        HspMember member = null;
        HspAttributeDimension dim = null;
        if (attributeDimensionsForBase != null) {
            for (int d = 0; d < attributeDimensionsForBase.size(); d++) {
                dim = attributeDimensionsForBase.elementAt(d);
                member = getDimMember(dim.getId(), memberName);
                if (member != null) {
                    return (member);
                }
            }
        }
        return (member);
    }

    public Vector<HspUserVariable> getUserVariables(int sessionId) {
        return getUserVariables(sessionId, false);
    }

    public Vector<HspUserVariable> getUserVariables(int sessionId, boolean filterOutContextSensitive) {
        Vector<HspUserVariable> vars = userVariableCache.getUnfilteredCache();
        if (filterOutContextSensitive) {
            for (int v = 0; v < vars.size(); v++) {
                HspUserVariable userVar = vars.elementAt(v);
                if ((userVar == null) || (userVar.isContextSensitive())) {
                    vars.remove(v);
                    v--;
                }
            }
        }
        return vars;
    }

    public Vector<HspUserVariable> getUserVariables(int dimId, int sessionId) {
        Vector<HspUserVariable> result = userVariableCache.getObjects(userVariableDimIdKeyDef, userVariableDimIdKeyDef.createKeyFromDimId(dimId));
        Vector<HspAttributeDimension> attrDims = getAttributeDimensionsForBaseDim(dimId);
        if (!HspUtils.isNullOrEmpty(attrDims)) {
            for (HspAttributeDimension attrDim : attrDims) {
                Vector<HspUserVariable> vars = userVariableCache.getObjects(userVariableDimIdKeyDef, userVariableDimIdKeyDef.createKeyFromDimId(attrDim.getDimId()));
                result.addAll(vars);
            }
        }
        return result;
    }

    public Vector<HspUserVariable> getUserVariables(int dimId, boolean includeNormal, boolean includeContextSensitive, int sessionId) {
        Vector<HspUserVariable> variablesForDim = getUserVariables(dimId, sessionId);
        Vector<HspUserVariable> variableList = null;
        if (variablesForDim != null) {
            for (HspUserVariable uv : variablesForDim) {
                boolean isSensitive = uv.isContextSensitive();
                if ((isSensitive && includeContextSensitive) || (!isSensitive && includeNormal)) {
                    if (variableList == null) {
                        variableList = new Vector<HspUserVariable>();
                    }
                    variableList.add(uv);
                }
            }
        }
        return (variableList);
    }

    public HspUserVariable getUserVariable(int variableId) {
        return userVariableCache.getObject(CachedObjectIdKeyDef.CACHED_OBJECT_ID_KEY, CachedObjectIdKeyDef.CACHED_OBJECT_ID_KEY.createKeyFromId(variableId));
    }

    public HspUserVariable getUserVariable(int dimId, String name) {
        //return userVariableCache.getObject(userVariableDimIdNameKeyDef, userVariableDimIdNameKeyDef.createKeyFromDimIdName(dimId, name));
        HspUserVariable userVar = userVariableCache.getObject(userVariableDimIdNameKeyDef, userVariableDimIdNameKeyDef.createKeyFromDimIdName(dimId, name));
        if (userVar == null) {
            HspDimension dimension = getDimRoot(dimId);
            if (dimension != null) {
                Vector<HspAttributeDimension> attributeDimensions = getAttributeDimensionsForBaseDim(dimId);
                if (attributeDimensions != null && attributeDimensions.size() > 0)
                    for (HspAttributeDimension attrDim : attributeDimensions) {
                        userVar = userVariableCache.getObject(userVariableDimIdNameKeyDef, userVariableDimIdNameKeyDef.createKeyFromDimIdName(attrDim.getDimId(), name));
                        if (userVar != null)
                            break;
                    }
            }
        }
        return userVar;
    }
    //Linear walk through user variables list, only use it for utility because of performance issue.

    public HspUserVariable getUserVariable(String name, int sessionId) {
        Vector<HspUserVariable> userVariables = this.getUserVariables(sessionId);
        if (userVariables != null) {
            for (int i = 0; i < userVariables.size(); i++) {
                HspUserVariable uv = userVariables.elementAt(i);
                if (uv != null) {
                    if (uv.getName().equalsIgnoreCase(name)) {
                        return uv;
                    }
                }
            }
        }
        return null;
    }

    public synchronized void addUserVariable(HspUserVariable userVariable, int sessionId) throws Exception {
        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        addUserVariable(userVariable, actionSet, sessionId);
        actionSet.doActions();
    }

    public synchronized void addUserVariable(HspUserVariable userVariable, HspActionSet actionSet, int sessionId) throws Exception {
        hspStateMgr.verify(sessionId);
        HspUserVariable existingVariable = getUserVariable(userVariable.getDimId(), userVariable.getVariableName());
        if (existingVariable != null)
            throw new DuplicateObjectException(userVariable.getName());
        HspUserVariableAction action = new HspUserVariableAction();
        actionSet.addAction(action, HspActionSet.ADD, userVariable);
    }

    public synchronized void updateUserVariable(HspUserVariable userVariable, int sessionId) throws Exception {
        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        updateUserVariable(userVariable, actionSet, sessionId);
        actionSet.doActions();
    }

    public synchronized void updateUserVariable(HspUserVariable userVariable, HspActionSet actionSet, int sessionId) throws Exception {
        hspStateMgr.verify(sessionId);
        HspUserVariableAction action = new HspUserVariableAction();
        actionSet.addAction(action, HspActionSet.UPDATE, userVariable);
    }

    public synchronized void deleteUserVariable(HspUserVariable userVariable, int sessionId) throws Exception {
        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        deleteUserVariable(userVariable, actionSet, sessionId);
        actionSet.doActions();
    }

    public synchronized void deleteUserVariable(HspUserVariable userVariable, HspActionSet actionSet, int sessionId) throws Exception {
        hspStateMgr.verify(sessionId);
        HspUserVariableAction action = new HspUserVariableAction();
        Vector userVariableValues = getUserVariableValues(userVariable.getVariableId(), sessionId);
        if (userVariableValues != null) {
            int size = userVariableValues.size();
            for (int i = 0; i < size; i++) {
                HspUserVariableValue userVariableValue = (HspUserVariableValue)userVariableValues.get(i);
                deleteUserVariableValue(userVariableValue, actionSet, sessionId);
            }
        }
        actionSet.addAction(action, HspActionSet.DELETE, userVariable);
    }

    public Vector getUserVariableValues(int variableId, int sessionId) {
        //TODO: Is this correct?
        return userVariableCache.getObjects(userVariableValueVariableIdKeyDef, userVariableValueVariableIdKeyDef.createKeyFromVariableId(variableId));
    }

    public Vector getUserVariableValues(int sessionId) {
        return userVariableCache.getUnfilteredCache();
    }

    public HspUserVariableValue getUserVariableValue(int userId, int variableId) {
        return userVariableValueCache.getObject(userVariableValueUserIdVariableIdKeyDef, userVariableValueUserIdVariableIdKeyDef.createKeyFromUserIdVariableId(userId, variableId));
    }

    public synchronized void addUserVariableValue(HspUserVariableValue userVariableValue, int sessionId) throws Exception {
        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        addUserVariableValue(userVariableValue, actionSet, sessionId);
        actionSet.doActions();
    }

    public synchronized void addUserVariableValue(HspUserVariableValue userVariableValue, HspActionSet actionSet, int sessionId) {
        hspStateMgr.verify(sessionId);
        HspUserVariableValueAction action = new HspUserVariableValueAction();
        actionSet.addAction(action, HspActionSet.ADD, userVariableValue);
    }

    public synchronized void updateUserVariableValue(HspUserVariableValue userVariableValue, int sessionId) throws Exception {
        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        updateUserVariableValue(userVariableValue, actionSet, sessionId);
        actionSet.doActions();
    }

    public synchronized void updateUserVariableValue(HspUserVariableValue userVariableValue, HspActionSet actionSet, int sessionId) {
        hspStateMgr.verify(sessionId);
        HspUserVariableValueAction action = new HspUserVariableValueAction();
        actionSet.addAction(action, HspActionSet.UPDATE, userVariableValue);
    }

    public synchronized void deleteUserVariableValue(HspUserVariableValue userVariableValue, int sessionId) throws Exception {
        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        deleteUserVariableValue(userVariableValue, actionSet, sessionId);
        actionSet.doActions();
    }

    public synchronized void deleteUserVariableValue(HspUserVariableValue userVariableValue, HspActionSet actionSet, int sessionId) {
        hspStateMgr.verify(sessionId);
        HspUserVariableValueAction action = new HspUserVariableValueAction();
        actionSet.addAction(action, HspActionSet.DELETE, userVariableValue);
    }

    public synchronized void setUserVariableValue(int userId, int variableId, HspMember member, int sessionId) throws Exception {
        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        setUserVariableValue(userId, variableId, member, actionSet, sessionId);
        actionSet.doActions();
    }

    public synchronized void setUserVariableValue(int userId, int variableId, HspMember member, HspActionSet actionSet, int sessionId) throws Exception {
        HspUserVariableValue existingValue = getUserVariableValue(userId, variableId);
        if (existingValue == null) {
            // If the member is null, ignore it, otherwise add it.
            if (member != null) {
                HspUserVariableValue newValue = new HspUserVariableValue();
                newValue.setUserId(userId);
                newValue.setVariableId(variableId);
                newValue.setMemberId(member.getId());
                addUserVariableValue(newValue, actionSet, sessionId);
            }
        } else {
            // If we have an existing value then we need to update it if the
            // member is not null or delete it if the member is null.
            if (member != null) {
                HspUserVariableValue newValue = (HspUserVariableValue)existingValue.cloneForUpdate();
                newValue.setMemberId(member.getId());
                updateUserVariableValue(newValue, actionSet, sessionId);
            } else {
                deleteUserVariableValue(existingValue, actionSet, sessionId);
            }
        }
    }

    public HspMember getSubstitutionVariableMember(String substVariableName, int baseDimId, int planType, int sessionId) {
        HspMember newMember = null;
        if (substVariableName != null) {
            newMember = createMember(baseDimId);
            newMember.setObjectName(substVariableName);
            newMember.setIdForCache(baseDimId);
            newMember.setDimId(baseDimId);
            newMember.memberId = baseDimId;
            newMember.setBaseMemberId(baseDimId);
            newMember.setObjectId(baseDimId);
            newMember.setObjectType(HspConstants.gObjType_SubstitutionVariable);
        }
        return (newMember);
    }

    public HspFormMember getSubstitutionVariableFormMember(HspFormMember fm, int planType, int sessionId) {
        HspMember substMember = getSubstitutionVariableMember(fm.getSubstVar(), fm.getDimId(), planType, sessionId);
        HspFormMember newFormMember = null;

        if (substMember != null) {
            newFormMember = new HspFormMember(substMember);
            newFormMember.setQueryType(fm.getQueryType());
            newFormMember.setSubstVar(fm.getSubstVar());
        }

        return (newFormMember);
    }

    private boolean isRangeInMemberList(int baseDimId, HspFormMember[] rtpMemberList, Vector<HspFormMember> rangeList) {
        if (rangeList != null && rangeList.size() > 0) {
            for (int i = 0; i < rangeList.size(); i++) {
                HspMember tempMem = getDimMember(baseDimId, rangeList.get(i).getMbrName());
                if (rtpMemberList != null && rtpMemberList.length != 0) {
                    boolean fnd = false;
                    for (int j = 0; j < rtpMemberList.length; j++) {
                        HspFormMember formMbr = rtpMemberList[j];
                        if (formMbr.getMbrId() == tempMem.getId()) {
                            fnd = true;
                            break;
                        }
                    }
                    if (!fnd)
                        return false;
                } else {
                    return true;
                }
            }
            return true;
        }
        return false;
    }


    public String[][] filterSubstVarFromHspFrmMemListAndDimId(HspFormMember[] rtpMemberList, String[][] substVariableNameValues, int baseDimId, int planType, int sessionId) {
        ArrayList<String[]> filterVarValues = new ArrayList<String[]>();
        //Check if any rtpMember is present.
        for (int i = 0; substVariableNameValues != null && i < substVariableNameValues.length; i++) {
            String substVariableValue = substVariableNameValues[i][1];
            if (substVariableValue != null) {
                // strip any quote off the front and back
                if (((substVariableValue.startsWith("\"")) && (substVariableValue.endsWith("\""))) || ((substVariableValue.startsWith("'")) && (substVariableValue.endsWith("'")))) {
                    substVariableValue = substVariableValue.substring(1, substVariableValue.length() - 1);
                }
                if (substVariableValue.length() > 0) {
                    substVariableValue = substVariableValue.trim();
                    HspMember tempMem = getDimMember(baseDimId, substVariableValue);
                    if (tempMem != null) {
                        //if rtpLimit is not given display all the valid substitute variable for the specified dim Id
                        if (rtpMemberList != null && rtpMemberList.length != 0) {
                            for (int j = 0; j < rtpMemberList.length; j++) {
                                HspFormMember member = rtpMemberList[j];
                                if (member.getMbrId() == tempMem.getId()) {
                                    filterVarValues.add(substVariableNameValues[i]);
                                    break;
                                }
                            }
                        } else {
                            filterVarValues.add(substVariableNameValues[i]);
                        }
                    } else {
                        try {
                            // check if we have a subst var range range
                            Vector<HspFormMember> formMbrs = null;
                            HspFormMember fm = new HspFormMember();
                            fm.setDimId(baseDimId);
                            if (substVariableValue.contains("::")) {
                                String[] rangeList = HspFMDBImpl.getVariableRange(substVariableValue, "::");
                                formMbrs = hspFMDB.getRangeMembers(rangeList, planType, fm, "::");
                                if (isRangeInMemberList(baseDimId, rtpMemberList, formMbrs))
                                    filterVarValues.add(substVariableNameValues[i]);
                            } else if (substVariableValue.contains(":")) {
                                String[] rangeList = HspFMDBImpl.getVariableRange(substVariableValue, ":");
                                formMbrs = hspFMDB.getRangeMembers(rangeList, planType, fm, ":");
                                if (isRangeInMemberList(baseDimId, rtpMemberList, formMbrs))
                                    filterVarValues.add(substVariableNameValues[i]);
                            }
                        } catch (Exception e) {
                            // skip this value
                            continue;
                        }
                    }
                }
            }
        }
        String[][] filterVarNamValArry = new String[filterVarValues.size()][];
        for (int i = 0; i < filterVarValues.size(); i++) {
            filterVarNamValArry[i] = filterVarValues.get(i);
        }
        return filterVarNamValArry;
    }

    public HspFormMember evaluateSubstitutionVariable(HspFormMember fm, int planType, int sessionId) {
        List<HspFormMember> mbrs = evaluateSubstitutionVariable2(fm, planType, sessionId);
        return !HspUtils.isNullOrEmpty(mbrs) ? mbrs.get(0) : null;
    }

    public List<HspFormMember> evaluateSubstitutionVariable2(HspFormMember fm, int planType, int sessionId) {
        List<HspFormMember> evaluatedMbrs = new ArrayList<HspFormMember>();
        String substVariableName = null;
        String substVariableValue = null;
        HspMember substMember = null;
        HspFormMember newFormMember = null;
        try {
            substVariableName = fm.getSubstVar();
            if (substVariableName.startsWith("&")) {
                substVariableName = substVariableName.substring(1);
            }

            substVariableValue = hspFMDB.essGetVariable(hspJS.getAppName(), planType, substVariableName, sessionId);
            if (substVariableValue != null) {
                // strip any quote off the front and back
                if (((substVariableValue.startsWith("\"")) && (substVariableValue.endsWith("\""))) || ((substVariableValue.startsWith("'")) && (substVariableValue.endsWith("'")))) {
                    substVariableValue = substVariableValue.substring(1, substVariableValue.length() - 1);
                }
                if (substVariableValue.length() > 0) {
                    Vector<HspFormMember> formMbrs = null;
                    if (substVariableValue.contains("::")) {
                        String[] rangeList = HspFMDBImpl.getVariableRange(substVariableValue, "::");
                        formMbrs = hspFMDB.getRangeMembers(rangeList, planType, fm, "::");
                        if (!HspUtils.isNullOrEmpty(formMbrs))
                            evaluatedMbrs.addAll(formMbrs);
                    } else if (substVariableValue.contains(":")) {
                        String[] rangeList = HspFMDBImpl.getVariableRange(substVariableValue, ":");
                        formMbrs = hspFMDB.getRangeMembers(rangeList, planType, fm, ":");
                        if (!HspUtils.isNullOrEmpty(formMbrs))
                            evaluatedMbrs.addAll(formMbrs);
                    } else {
                        substMember = getDimMember(fm.getDimId(), substVariableValue);
                        if ((substMember != null) && (substVariableValue != null)) {
                            newFormMember = new HspFormMember(substMember);
                            int order = newFormMember.getOrder();
                            newFormMember.copySettings(fm);
                            // Reset subvar variable to null to avoid reevaluation of substVar
                            newFormMember.setSubstVar(null);
                            newFormMember.setOrder(order); // Reinstate the original order or valid combinations will NOT work!
                            evaluatedMbrs.add(newFormMember);
                        }
                    }
                }
            }
        } catch (Exception e) {
            HspLogger.LogException(e);
            substVariableValue = null;
            substMember = null;
            newFormMember = null;
            evaluatedMbrs.clear();
        }

        return evaluatedMbrs;
    }

    public HspMember evaluateSubstitutionVariable(HspMember member, int planType, int sessionId) {
        String substVariableValue = null;
        String substVariableName = null;
        HspMember substMember = null;
        try {
            if (member.getObjectType() == HspConstants.gObjType_SubstitutionVariable) {
                substVariableName = member.getName();
                if (substVariableName.startsWith("&")) {
                    substVariableName = substVariableName.substring(1);
                }
                substVariableValue = hspFMDB.essGetVariable(hspJS.getAppName(), planType, substVariableName, sessionId);
                if (substVariableValue != null) {
                    // strip any quote off the front and back
                    if (((substVariableValue.startsWith("\"")) && (substVariableValue.endsWith("\""))) || ((substVariableValue.startsWith("'")) && (substVariableValue.endsWith("'")))) {
                        substVariableValue = substVariableValue.substring(1, substVariableValue.length() - 1);
                    }
                    if (substVariableValue.length() > 0) {
                        substMember = getDimMember(member.getDimId(), substVariableValue);
                    }
                } else {
                    //If the member comes up to be null, then the variable could be user variable
                    String userVarName = member.getName();
                    if (userVarName.startsWith("&"))
                        userVarName = userVarName.substring(1);

                    HspUserVariable userVariable = getUserVariable(member.getDimId(), userVarName);
                    if (userVariable != null) {
                        HspUserVariableValue userVariableValue = getUserVariableValue(hspStateMgr.getUserId(sessionId), userVariable.getVariableId());
                        if (userVariableValue != null)
                            substMember = getDimMember(member.getDimId(), userVariableValue.getMemberId());
                    }
                }
            }
        } catch (Exception e) {
            //HspLogger.LogException(e);
            logger.finer(e.getMessage());
            substVariableValue = null;
            substMember = null;
        }
        return (substMember);
    }

    public int getNumAllDimensions(int planTypes, int sessionId) {
        Vector<HspDimension> dims = getAllDimensions(planTypes, sessionId);
        if (dims == null)
            return 0;
        return dims.size();
    }

    public int getNumDimensions(int sessionId) {
        Vector<HspDimension> baseDimensions = getBaseDimensions(sessionId);
        if ((baseDimensions == null) || (baseDimensions.size() <= 0)) {
            return 0;
        } else {
            return (baseDimensions.size());
        }
    }

    public int getNumBaseDimensions(int planTypes, int sessionId) {
        Vector<HspDimension> dims = getBaseDimensions(planTypes, sessionId);
        if (dims == null)
            return 0;
        return dims.size();
    }

    public int getNumAttributeDimensions(int planTypes, int sessionId) {
        Vector<HspAttributeDimension> dims = getAttributeDimensions(sessionId);
        if (dims == null)
            return 0;
        return dims.size();
    }

    public <MBR extends HspMember> Vector<MBR> getChildMembers(int dimId, int parentId, int sessionId) {
        return getChildMembers(dimId, parentId, true, sessionId);
    }

    private <MBR extends HspMember> Vector<MBR> getChildMembersWithoutMemberPTProps(int dimId, int parentId, int sessionId) {
        // NOTE: use this method with care. It does not bind memberPTPRops to members. Aside from not having that information
        // available, saving the members as they are returned will result in errors. MemberPTProps MUST be rebound to the
        // members before saving (or placing in an actionset). For almost all uses the getChildMembers call that returns
        // ptMemberProps bound should be used. This method is used to avoid the expensive member cloning that occurs to bind the ptMemberProps.
        return getChildMembers(dimId, parentId, false, sessionId);
    }

    private <MBR extends HspMember> Vector<MBR> getChildMembers(int dimId, int parentId, boolean bindMemberPTPRops, int sessionId) {
        // Note comments for wrapper method that calls this with the bindMemberPTPRops argument set to false
        // We no longer verify session on this fine of a level due to the performance impact
        //hspStateMgr.verify(sessionId);
        //return (Vector<MBR>) getMembersCache(dimId).getChildren(parentId);
        Vector<MBR> members = (Vector<MBR>)getMembersCache(dimId).getChildren(parentId);
        if (members != null && bindMemberPTPRops)
            for (int i = 0; i < members.size(); i++) {
                MBR member = members.get(i);
                if (member != null) {
                    Vector<HspMemberPTProps> props = mbrPTPropsCache.getObjects(mbrPTPropsMemberIdKeyDef, mbrPTPropsMemberIdKeyDef.createKeyFromId(member.getId()));
                    if (props != null) {
                        if (member.isReadOnly())
                            member = (MBR)member.cloneForUpdate();
                        member.setMemberPTPropsToBeSaved(props);
                        member.clearClonedFrom();
                        member.makeReadOnly();
                        members.set(i, member);
                    }
                }
            }
        return (Vector<MBR>)members;
    }

    private <MBR extends HspMember> Vector<MBR> getChildMembers(int dimId, int parentId, boolean bindMemberPTPRops, HspActionSet actionSet, int sessionId) {
        Vector<MBR> children = getChildMembers(dimId, parentId, bindMemberPTPRops, sessionId);
        if (actionSet != null && parentId > 0) {
            Predicate childOfParentPredicate = PredicateUtils.andPredicate(PredicateUtils.instanceofPredicate(HspMember.class), HspPredicateUtils.keyDefPredicate(cachedObjectParentIdKeyDef, cachedObjectParentIdKeyDef.createKeyFromParentId(parentId)));
            List<DualVal<CachedObject, ActionMethod>> objectAndMethodList = actionSet.findCachedObjects(childOfParentPredicate, ActionMethod.ADD, ActionMethod.DELETE, ActionMethod.MOVE, ActionMethod.UPDATE, ActionMethod.UPDATE_ADDING_IF_NEEDED);
            if (HspUtils.size(objectAndMethodList) <= 0)
                return children;
            KeyDef keyDef = CachedObjectIdKeyDef.CACHED_OBJECT_ID_KEY;
            Map<Object, MBR> childrenByIdMap = HspUtils.addToMap(children, keyDef, new LinkedHashMap<Object, MBR>());
            for (DualVal<CachedObject, ActionMethod> objectAndMethod : objectAndMethodList) {
                MBR child = (MBR)objectAndMethod.getVal1();
                if (objectAndMethod.getVal2() == ActionMethod.DELETE)
                    childrenByIdMap.remove(keyDef.createKey(child));
                else
                    childrenByIdMap.put(keyDef.createKey(child), child);
            }
            children = new Vector<MBR>(childrenByIdMap.values());
        }
        return children;

    }


    public Vector<HspAccount> getAccounts(int sessionId) {
        return accountCache.getFilteredCache(hspStateMgr.getUserId(sessionId), HspConstants.ACCESS_READ | HspConstants.ACCESS_WRITE);
    }

    public Vector<HspEntity> getEntities(int sessionId) {
        return entityCache.getFilteredCache(hspStateMgr.getUserId(sessionId), HspConstants.ACCESS_READ | HspConstants.ACCESS_WRITE);
    }

    public Vector<HspEntity> getWritableEntities(int sessionId) {
        return entityCache.getFilteredCache(hspStateMgr.getUserId(sessionId), HspConstants.ACCESS_WRITE);
    }

    public Vector<HspEntity> getChildEntities(int parentId, int sessionId) {
        if (parentId == HspJS.ROOT_NODE)
            return entityCache.getChildrenOfRoot();
        else
            return entityCache.getChildren(parentId);
    }


    //Single object read methods

    public int getUsedIn(int dimId, int mbrId) {
        HspMember member = getDimMemberWithoutPtProps(dimId, mbrId);
        return getUsedIn(member);
    }

    public int getUsedIn(HspMember member) {
        //Get used in has different implementations for Accounts, Entities and Custom Dimensions
        //If the member doesnt exist, it isnt used in any cube
        if (member == null)
            return HspConstants.PLAN_TYPE_NONE;
        return member.getUsedIn();
    }

    public void setUsedIn(HspMember member, int usedIn) {
        //Get used in has different implementations for Accounts, Entities and Custom Dimensions
        //If the member doesnt exist, it isnt used in any cube
        if (member == null)
            return; // HspConstants.PLAN_TYPE_NONE;
        member.setUsedIn(usedIn);
    }

    public boolean hasChildrenInPlan(int dimId, int mbrId, int planType, int sessionId) {
        HspMember member = getDimMember(dimId, mbrId);
        Vector<HspMember> children = member == null ? null : member.getChildren();
        if (children != null && children.size() > 0) {
            for (HspMember mem : children) {
                int usedIn = getUsedIn(mem);
                if (((usedIn & planType) > 0) || (planType < 0)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasChildren(int dimId, int mbrId) {
        HspMember member = getDimMember(dimId, mbrId);
        Vector<HspMember> children = member == null ? null : member.getChildren();
        if (children != null && children.size() > 0) {
            return true;
        }
        return false;
    }

    public HspAccount getAccount(int accountId) {
        return (HspAccount)fillMemberPTProps(accountCache.getObject(accountId));
    }

    public HspAccount getAccount(String accountName) {
        return (HspAccount)fillMemberPTProps(accountCache.getObject(accountName));
    }

    public HspEntity getEntity(int entityId) {
        return (HspEntity)fillMemberPTProps(entityCache.getObject(entityId));
    }

    public HspEntity getEntity(String entityName) {
        return (HspEntity)fillMemberPTProps(entityCache.getObject(entityName));
    }

    public HspScenario getScenario(int scenarioId) {
        return (HspScenario)fillMemberPTProps(scenarioCache.getObject(scenarioId));
    }

    public HspScenario getScenario(String scenarioName) {
        return (HspScenario)fillMemberPTProps(scenarioCache.getObject(scenarioName));
    }

    public HspVersion getVersion(int versionId) {
        return (HspVersion)fillMemberPTProps(versionCache.getObject(versionId));
    }

    public HspVersion getVersion(String versionName) {
        return (HspVersion)fillMemberPTProps(versionCache.getObject(versionName));
    }

    public Vector<HspDriverMember> getDriverMembers(int baseDimId) {
        return driverMemberCache.getObjects(driverMemberBaseDimIdKeyDef, driverMemberBaseDimIdKeyDef.createKeyFromBaseDimId(baseDimId));
    }

    public Vector<HspDriverMember> getDriverMembers(int baseDimId, int loadId) {
        Vector<HspDriverMember> v = driverMemberCache.getObjects(driverMemberBaseDimIdKeyDef, driverMemberBaseDimIdKeyDef.createKeyFromBaseDimId(baseDimId));
        Vector<HspDriverMember> rv = new Vector<HspDriverMember>();
        if (v != null)
            for (int i = 0; i < v.size(); i++)
                if (v.get(i).getLoadId() == loadId)
                    rv.add(v.get(i));
        return rv;
    }

    public Vector<HspDriverMember> getEvaluatedDriverMembers(int baseDimId, int loadId, int planType, int sessionId) {
        Vector<HspDriverMember> v = getEvaluatedDriverMembers(baseDimId, planType, sessionId);
        Vector<HspDriverMember> rv = new Vector<HspDriverMember>();
        if (v != null)
            for (int i = 0; i < v.size(); i++)
                if (v.get(i).getLoadId() == loadId)
                    rv.add(v.get(i));
        return rv;
    }

    public Vector<HspDriverMember> getEvaluatedDriverMembers(int baseDimId, int planType, int sessionId) {
        Vector<HspDriverMember> driverMembers = driverMemberCache.getObjects(driverMemberBaseDimIdKeyDef, driverMemberBaseDimIdKeyDef.createKeyFromBaseDimId(baseDimId));

        if (driverMembers != null && !driverMembers.isEmpty()) {
            Vector<HspDriverMember> originalMembers = driverMembers;
            driverMembers = new Vector<HspDriverMember>();
            for (int i = 0; i < originalMembers.size(); i++) {
                HspDriverMember driverMember = originalMembers.get(i);
                if (driverMember.getQueryType() == HspConstants.ESS_QUERY_UNSPECIFIED)
                    driverMembers.add(driverMember);
                else {
                    HspFormMember formMember = createFormMember(driverMember);
                    Vector<HspFormCell> cells = hspFMDB.evaluateFunction(new HspFormCell(formMember), driverMember.getQueryType(), planType, false, sessionId);
                    for (int j = 0; j < cells.size(); j++) {
                        HspFormCell cell = cells.get(j);
                        if (cell != null)
                            driverMembers.add(createDriverMember(cell, driverMember.getBaseDimId()));
                    }
                }
            }
        }
        return driverMembers;
    }


    public synchronized void setDriverMembers(int baseDimensionId, int driverDimensionId, HspDriverMember[] driverMembers, int sessionId) throws Exception {
        setDriverMembers(baseDimensionId, driverDimensionId, driverMembers, new HspLineItemMember[] { }, sessionId);
    }

    public synchronized void setDriverMembers(int baseDimensionId, int driverDimensionId, HspDriverMember[] driverMembers, HspLineItemMember[] lineItemMembers, int sessionId) throws Exception {
        setDriverMembers(baseDimensionId, driverDimensionId, 0, driverMembers, lineItemMembers, sessionId);
    }

    public synchronized void setDriverMembers(int baseDimensionId, int driverDimensionId, int loadId, HspDriverMember[] driverMembers, HspLineItemMember[] lineItemMembers, int sessionId) throws Exception {
        setDriverMembers(baseDimensionId, driverDimensionId, loadId, driverMembers, lineItemMembers, true, sessionId);
    }

    public synchronized void setDriverMembers(int baseDimensionId, int driverDimensionId, int loadId, HspDriverMember[] driverMembers, HspLineItemMember[] lineItemMembers, boolean setLineItemMembers, int sessionId) throws Exception {
        // driverMember.loadId is set on every driver member with the loadId argument in the action layer
        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        HspActionSet actionSet = new HspActionSet(hspSQL, user);

        HspDimension baseDimension = getDimRoot(baseDimensionId);
        if (baseDimension == null) {
            IllegalArgumentException x = new IllegalArgumentException("Invalid baseDimensionId argument.");
            logger.throwing(x);
            throw x;
        }
        HspDimension driverDimension = getDimRoot(driverDimensionId);
        if (driverDimension == null) {
            IllegalArgumentException x = new IllegalArgumentException("Invalid driverDimensionId argument.");
            logger.throwing(x);
            throw x;
        }

        // since saving a dimension can cause a lot of cache flushes make sure that we need to do it
        // only save if the driver dimension id needs changing on the base dimension
        if (loadId == 0 && (baseDimension.getDriverDimId() != driverDimensionId)) {
            baseDimension = (HspDimension)baseDimension.cloneForUpdate();
            baseDimension.setDriverDimId(driverDimensionId);
            saveDimension(baseDimension, actionSet, sessionId);
        }


        setDriverMembers(baseDimensionId, driverDimensionId, loadId, driverMembers, actionSet, sessionId);

        // for now set lineItem info for the default (loadId == 0) ONLY, and this would be through the Data Load Admin UI. This is the default and what adapters would use.
        // this would need to be changed if run-time definition of lineItemMemers by loadId is desired.
        if (loadId == 0)
            setLineItemMembers(baseDimensionId, loadId, lineItemMembers, actionSet);

        //Next we execute the action set
        actionSet.doActions();
        //Cache will be automatically updated/invalidated by the HspAction class
    }

    public HspDriverMember addInitLoadDriverMember(int sessionId) throws Exception {
        // This method is used to obtain a loadId for subsequent setDriverMember calls
        // see hspDEDB.setDriverMembers calls for adding/updating/deleting driver members
        // This is a dummy driver member whicy should never be referenced for loads as it references the 'rates' dimensin
        HspDriverMember driver = new HspDriverMember();
        driver.setBaseDimId(HspConstants.kDimensionRates);
        driver.setDimId(HspConstants.kDimensionRates);
        driver.setMemberId(HspConstants.kDimensionRates);
        driver.setPosition(0);
        driver.setQueryType(0);
        return addDriverMember(driver, sessionId);
    }

    private HspDriverMember addDriverMember(HspDriverMember driver, int sessionId) throws Exception {
        // This method is used to obtain a loadId for subsequent setDriverMember calls by inserting a dummy record
        // see hspDEDB.setDriverMembers calls for adding/updating/deleting driver members
        // This method will not load a record with  loadId = 0, these are the app-wide set defined in data load admin
        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        HspDriverMemberAction action = new HspDriverMemberAction();
        actionSet.addAction(action, HspActionSet.ADD, driver);
        actionSet.doActions();
        return driver;
    }

    public void deleteDriverMembersByLoadId(int loadId, int sessionId) throws Exception {
        // This method is used to delete ALL driver members for a given loadId at the end of an OLU process
        // DO NOT allow deletion of loadId = 0 drivers, these are the app-wide set defined in data load admin
        // see hspDEDB.setDriverMembers calls for adding/updating/deleting driver members
        if (loadId != 0) {
            HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
            HspActionSet actionSet = new HspActionSet(hspSQL, user);
            HspDriverMemberAction action = new HspDriverMemberAction();
            HspDriverMember driver = new HspDriverMember();
            driver.setLoadId(loadId);
            actionSet.addAction(action, HspActionSet.DELETE, driver);
            actionSet.doActions();
        }
        return;
    }

    private synchronized void setLineItemMembers(int baseDimensionId, int loadId, HspLineItemMember[] lineItemMembers, HspActionSet actionSet) {
        Vector<HspLineItemMember> existingLineItemMembers = getLineItemMembers(baseDimensionId);
        HspAction action = new HspLineItemMembeCustomAction(baseDimensionId, loadId, (existingLineItemMembers == null) ? null : existingLineItemMembers.toArray(new HspLineItemMember[0]));
        actionSet.addAction(action, actionSet.CUSTOM, lineItemMembers);
    }

    private synchronized void setDriverMembers(int baseDimensionId, int driverDimensionId, int loadId, HspDriverMember[] driverMembers, HspActionSet actionSet, int sessionId) throws Exception {
        // validation of query type is deferred to evaluate query method.
        // driverMember.loadId is set on every driver member with the loadId argument in the action layer
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(HspDriverMemberAction.KEY_DRIVER_MEMBER_LOAD_ID, new Integer(loadId));
        HspAction action = new HspDriverMemberCustomAction(baseDimensionId, loadId);
        actionSet.addAction(action, actionSet.CUSTOM, driverMembers, map);
    }

    public HspDriverMember createDriverMember(HspFormMember formMember, int baseDimensionId) {
        // The baseDim provided is not the same as formMember.baseDim which is for attributes.
        HspDriverMember driver = new HspDriverMember();
        driver.setBaseDimId(baseDimensionId);
        driver.setDimId(formMember.getDimId());
        driver.setMemberId(formMember.getMbrId());
        driver.setPosition((int)formMember.getOrdinal());
        driver.setQueryType(formMember.getQueryType());
        return driver;
    }

    public Vector<HspDriverMember> getAllDriverMembers() {
        return driverMemberCache.getUnfilteredCache();
    }


    public HspFormMember createFormMember(HspDriverMember driver) {
        HspMember member = this.getDimMember(driver.getDimId(), driver.getMemberId());
        if (member == null) {
            IllegalStateException x = new IllegalStateException("Driver member argument does not reference a valid or existant member.");
            logger.throwing(x);
            throw x;
        }
        HspFormMember formMember = new HspFormMember(member);
        HspDimension dimension = getDimRoot(driver.getBaseDimId());
        if (dimension == null) {
            IllegalStateException x = new IllegalStateException("Driver member argument does not reference a valid or existant base dimension.");
            logger.throwing(x);
            throw x;
        }
        formMember.setQueryType(driver.getQueryType());
        formMember.setOrdinal(driver.getPosition());
        return formMember;
    }

    public HspCube getFirstCube() {
        Vector<HspCube> cubes = getCubes(internalSessionId);
        if ((cubes == null) || (cubes.size() <= 0))
            return null;
        return cubes.elementAt(0);
    }

    public Vector<HspCube> getCubes(int sessionId) {
        return getCubes(true, sessionId);
    }

    public Vector<HspCubeSLMapping> getCubeSLMappingsForCube(int cubeId, int sessionId) {
        hspStateMgr.verify(sessionId);
        Vector<HspCubeSLMapping> cubeSLMappings = cubeSLMappingCache.getUnfilteredCache();
        Vector<HspCubeSLMapping> filteredCubeSLMappings = new Vector<HspCubeSLMapping>();
        for (HspCubeSLMapping mapping : cubeSLMappings) {
            if (mapping.getCubeId() == cubeId)
                filteredCubeSLMappings.add(mapping);
        }
        return filteredCubeSLMappings;
    }

    public Vector<HspDPCubeMapping> getDPCubeMappingsForMappingType(int mappingTypeId, int sessionId) {
        hspStateMgr.verify(sessionId);
        Vector<HspDPCubeMapping> cubeMappings = cubeMappingCache.getUnfilteredCache();
        Vector<HspDPCubeMapping> filteredCubeMappings = new Vector<HspDPCubeMapping>();
        for (HspDPCubeMapping mapping : cubeMappings) {
            if (mapping.getMappingTypeId() == mappingTypeId)
                filteredCubeMappings.add(mapping);
        }
        return filteredCubeMappings;

    }

    public Vector<HspDPCubeMapping> getDPCubeMappingsForMappingTypes(List<Integer> mappingTypeList, int sessionId) {
        hspStateMgr.verify(sessionId);
        Vector<HspDPCubeMapping> cubeMappings = cubeMappingCache.getUnfilteredCache();
        Vector<HspDPCubeMapping> filteredCubeMappings = new Vector<HspDPCubeMapping>();
        if (mappingTypeList != null) {
            for (HspDPCubeMapping mapping : cubeMappings) {
                if (mappingTypeList.contains(mapping.getMappingTypeId()))
                    filteredCubeMappings.add(mapping);
            }
        }
        return filteredCubeMappings;
    }


    public Vector<HspCube> getCubes(boolean filterReporting, int sessionId) {
        hspStateMgr.verify(sessionId);
        Vector<HspCube> cubes = cubeCache.getUnfilteredCache();
        if (filterReporting) {
            HspUtils.filterList(cubes, new HspNonEssbaseCubeTypeFilter());
        }
        return cubes;
    }

    public Vector<HspCube> getCubesForDelete(int sessionId) {
        hspStateMgr.verify(sessionId);
        GenericCache<HspPendingXacts> cache = new GenericCache<HspPendingXacts>(new JDBCCacheLoader<HspPendingXacts>(HspPendingXacts.class, "SQL_GET_CUBES_FOR_DELETE", hspSQL), hspSecDB);
        Vector<HspCube> cubes = new Vector<HspCube>();
        if (cache != null) {
            Vector<HspPendingXacts> xacts = cache.getUnfilteredCache();
            for (int i = 0; i < xacts.size(); i++) {
                HspPendingXacts xact = xacts.get(i);
                HspCube cube = new HspCube();
                cube.setType(xact.getSrcMbr());
                cube.setCubeName(xact.getCubeName());
                cube.setAppName(xact.getAppName());
                cube.setPlanType(xact.getPlanType());
                cubes.add(cube);
            }
        }
        return cubes;
    }


    public HspCube getCube(int cubeId) {
        return cubeCache.getObject(cubeId);
    }

    public Vector<HspCube> getEssbaseCubes(int sessionId) {
        hspStateMgr.verify(sessionId);
        return cubeCache.getObjects(cubeTypeKeyDef, cubeTypeKeyDef.createKeyFromCubeType(HspConstants.REPORTING_CUBE));
    }

    public HspCube getEssbaseCube(int cubeId, int sessionId) {
        hspStateMgr.verify(sessionId);
        Vector<HspCube> cubes = cubeCache.getObjects(cubeTypeKeyDef, cubeTypeKeyDef.createKeyFromCubeType(HspConstants.REPORTING_CUBE));
        if ((cubes == null) || (cubes.size() <= 0))
            return null;
        else
            for (int loop1 = 0; loop1 < cubes.size(); loop1++) {
                HspCube cube = cubes.elementAt(loop1);
                if ((cube != null) && (cube.getCubeId() == cubeId))
                    return cube;
            }
        return null;
    }

    public HspCube getEssbaseCube(String cubeName, int sessionId) {
        hspStateMgr.verify(sessionId);
        if (cubeName == null || cubeName.length() == 0)
            return null;
        Vector<HspCube> cubes = cubeCache.getObjects(cubeTypeKeyDef, cubeTypeKeyDef.createKeyFromCubeType(HspConstants.REPORTING_CUBE));
        if ((cubes == null) || (cubes.size() <= 0))
            return null;
        else
            for (int loop1 = 0; loop1 < cubes.size(); loop1++) {
                HspCube cube = cubes.elementAt(loop1);
                if ((cube != null) && (cubeName.equalsIgnoreCase(cube.getCubeName())))
                    return cube;
            }
        return null;
    }

    public HspCube getCubeByCubeName(String cubeName) {
        // we dont use the call below since object name of REPORTING_CUBES are not the real cube name
        //return cubeCache.getObject(cubeName);
        if (cubeName == null || cubeName.length() == 0)
            return null;
        //cubeName may not be unique across supported Plan Types
        //therefore restricting this is Planning generated cubes (BSO/ASO)
        Vector<HspCube> cubes = getCubes(internalSessionId);
        if (cubes != null) {
            for (int loop1 = 0; loop1 < cubes.size(); loop1++) {
                HspCube curCube = cubes.elementAt(loop1);
                if ((curCube != null) && (cubeName.equalsIgnoreCase(curCube.getCubeName()))) {
                    return curCube;
                }
            }
        }
        return null;


    }

    public HspCube getCubeByPlanTypeName(String planTypeName) {
        if (planTypeName == null)
            return null;
        //assumption is planTypeName is unique for a given Planning application
        //across supported Plan Types (ASO/BSO/Reporting)
        Vector<HspCube> cubes = getCubes(false, internalSessionId);
        if (cubes != null) {
            for (int loop1 = 0; loop1 < cubes.size(); loop1++) {
                HspCube curCube = cubes.elementAt(loop1);
                if ((curCube != null) && (planTypeName.equalsIgnoreCase(curCube.getPlanTypeName()))) {
                    return curCube;
                }
            }
        }
        return null;
    }

    public HspDimension getDimensionFromAlias(int planType, String aliasDimName, int alsTableId, int sessionId) {
        Vector<HspDimension> dims = getAllDimensions(planType, sessionId);
        for (HspDimension dim : dims) {
            if (dim.isPMMember())
                continue;

            HspAlias alias = hspAlsDB.getAlias(alsTableId, dim);
            if (alias != null && aliasDimName.equalsIgnoreCase(alias.getName())) {
                return dim;
            }
        }
        return null;
    }

    /**
     * Returns the cube with the specified cube name.  If no cubes are found with this name,
     * Then the cube wiht this Plan Type name is returned.
     */
    public HspCube getCube(String name) {
        HspCube cube = getCubeByCubeName(name);
        if (cube == null)
            cube = getCubeByPlanTypeName(name);
        return cube;
    }

    public String getCubeName(int cubeId) {
        HspCube cube = getCube(cubeId);
        if (cube == null)
            return null;
        return cube.getName();
    }

    public String getCubeNameByPlanType(int planTypeId) { //Session verified by getCubeByPlanType
        HspCube cube = getCubeByPlanType(planTypeId);
        if (cube == null)
            return null;
        return cube.getName();
    }

    public HspCube getCubeByPlanType(int planTypeId) {
        Vector<HspCube> cubes = cubeCache.getUnfilteredCache();
        if ((cubes == null) || (cubes.size() <= 0))
            return null;
        else
            for (int loop1 = 0; loop1 < cubes.size(); loop1++) {
                HspCube cube = cubes.elementAt(loop1);
                if ((cube != null) && (cube.getPlanType() == planTypeId))
                    return cube;
            }
        return null;
    }

    public HspCube getEssbaseCubeByPlanType(int planTypeId) {
        Vector<HspCube> cubes = cubeCache.getObjects(cubeTypeKeyDef, cubeTypeKeyDef.createKeyFromCubeType(HspConstants.REPORTING_CUBE));
        if ((cubes == null) || (cubes.size() <= 0))
            return null;
        else
            for (int loop1 = 0; loop1 < cubes.size(); loop1++) {
                HspCube cube = cubes.elementAt(loop1);
                if ((cube != null) && (cube.getPlanType() == planTypeId))
                    return cube;
            }
        return null;
    }

    public HspCube getEssbaseCubeByPlanTypeName(String planTypeName) {
        if (planTypeName == null)
            return null;
        Vector<HspCube> cubes = cubeCache.getObjects(cubeTypeKeyDef, cubeTypeKeyDef.createKeyFromCubeType(HspConstants.REPORTING_CUBE));
        if (cubes != null) {
            for (int loop1 = 0; loop1 < cubes.size(); loop1++) {
                HspCube curCube = cubes.elementAt(loop1);
                if ((curCube != null) && (planTypeName.equalsIgnoreCase(curCube.getPlanTypeName()))) {
                    return curCube;
                }
            }
        }
        return null;
    }


    public Vector<HspCube> getCubesByUsedIn(int usedIn, int sessionId) {
        return getCubesByUsedIn(usedIn, false, sessionId);
    }

    private Vector<HspCube> getCubesByUsedIn(int usedIn, boolean skipASO, int sessionId) {
        Vector<HspCube> allCubes = getCubes(sessionId);
        Vector<HspCube> usedCubes = new Vector<HspCube>(allCubes.size());
        for (int i = 0; i < allCubes.size(); i++) {
            HspCube cube = allCubes.get(i);
            if (cube != null && (cube.getPlanType() & usedIn) == cube.getPlanType()) {
                if (skipASO && cube.getType() == HspConstants.ASO_CUBE)
                    ;
                else
                    usedCubes.add(cube);
            }
        }
        return usedCubes;
    }

    public synchronized void addCube(HspCube newCube, int sessionId) throws Exception {
        hspStateMgr.verify(sessionId);
        validateCube(newCube);
        // try to find 1st available plantype for cube.
        if (newCube.getPlanType() < 1) {
            int minPT = newCube.getType() == HspConstants.REPORTING_CUBE ? HspConstants.gObject_1stEssbaseCubePlanType : 1;
            int maxPT = newCube.getType() != HspConstants.REPORTING_CUBE ? HspConstants.gObject_1stEssbaseCubePlanType : HspConstants.ALL_PLAN_TYPES;

            for (int nxtPT = minPT; nxtPT < maxPT; nxtPT = nxtPT << 1) {
                HspCube cube = getCubeByPlanType(nxtPT);
                if (cube == null)
                    cube = getEssbaseCubeByPlanType(nxtPT);
                if (cube == null) {
                    newCube.setPlanType(nxtPT);
                    break;
                }
            }
            if (newCube.getPlanType() < 1)
                throw new HspRuntimeException("ERROR_MSG_MAX_REP_PLAN_TYPE_LIMIT_REACHED");
        }
        // update system config for newly added plan type
        HspSystemCfg sysCfg = hspJS.getSystemCfg();
        sysCfg = (HspSystemCfg)sysCfg.cloneForUpdate();
        sysCfg.setPlanTypes(sysCfg.getPlanTypes() | newCube.getPlanType());

        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        HspCubeAction cubeAction = new HspCubeAction();
        actionSet.addAction(cubeAction, HspActionSet.ADD, newCube);
        actionSet.addAction(new HspSystemCfgAction(), HspActionSet.UPDATE, sysCfg);
        actionSet.doActions();
    }
    /*Method added to update cache when cube rename happens*/

    public synchronized void updateCube(HspCube newCube, int sessionId) throws Exception {
        hspStateMgr.verify(sessionId);
        validateCube(newCube);
        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        HspCubeAction cubeAction = new HspCubeAction();
        actionSet.addAction(cubeAction, HspActionSet.UPDATE, newCube);
        actionSet.doActions();
    }

    private void validateCube(HspCube cube) {
        HspUtils.verifyArgumentNotNull(cube, "cube");
        switch (cube.getType()) {
        case HspConstants.WORKFORCE_CUBE:
            break;
        case HspConstants.CAPEX_CUBE:
            break;
        case HspConstants.GENERIC_CUBE:
            break;
        case HspConstants.REPORTING_CUBE:
            break;
        case HspConstants.PROJECT_CUBE:
            break;
        case HspConstants.HSF_CUBE:
            break;
        case HspConstants.FINSTMT_CUBE:
            break;
        case HspConstants.ASO_CUBE:
            break;
        case HspConstants.WFSC_CUBE:
            break;
            // case HspConstants.EPBCSREP_CUBE:break;  TODO: fix this later
        default:
            throw new RuntimeException("Invalid cube type specified: " + cube.getType());


        }
        if (cube.getCubeName().length() > HspConstants.MAX_CUBENAME_LENGTH) {
            Properties props = new Properties();
            props.setProperty("CUBE_NAME", cube.getCubeName());
            HspRuntimeException x = new HspRuntimeException("MSG_CUBE_NAME_INVALID", props);
            logger.throwing(x);
            throw x;
        }
        if ((cube.getLocationAlias() != null) && (cube.getLocationAlias().length() > HspConstants.MAX_LOCATION_ALIAS_LENGTH)) {
            RuntimeException x = new RuntimeException("Location Alias on \"" + cube.getCubeName() + "\" cube exceeds maximum length (" + HspConstants.MAX_LOCATION_ALIAS_LENGTH + "): " + cube.getLocationAlias());
            logger.throwing(x);
            throw x;
        }
    }

    public void checkForReferencingObjects(HspCube cube, int sessionId) throws Exception {
        hspStateMgr.verify(sessionId);
        if (cube == null)
            return;
        Vector<HspForm> forms = hspFMDB.getFormsByCubeId(cube.getCubeId(), sessionId);
        if (forms != null && forms.size() > 0) {
            Properties p = new Properties();
            p.put("PLAN_TYPE", cube.getPlanTypeName());
            throw new HspRuntimeException("MSG_CUBE_FORM_REFERENCE", p);
        }
        if (hspFMDB.hasCellContent(cube)) {
            Properties p = new Properties();
            p.put("PLAN_TYPE", cube.getPlanTypeName());
            throw new HspRuntimeException("MSG_CUBE_CELL_CONTENT_REFERENCE", p);
        }

        HspCalcMgrRule[] rules = hspJS.getCalcMgrDB(sessionId).getRules(cube.getPlanTypeName());
        if (!HspUtils.isNullOrEmpty(rules)) {
            Properties p = new Properties();
            p.put("PLAN_TYPE", cube.getPlanTypeName());
            throw new HspRuntimeException("MSG_CUBE_BUSINESS_RULE_REFERENCE", p);
        }

        List<HspPMDimension> pmDims = hspPMDB.getPMDimensionsReferencingCube(cube, sessionId);
        if (!HspUtils.isNullOrEmpty(pmDims)) {
            Properties p = new Properties();
            p.put("PLAN_TYPE", cube.getPlanTypeName());
            throw new HspRuntimeException("MSG_CUBE_PMDIM_REFERENCE", p);
        }

        Vector<HspCubeLink> cubeLinks = hspCubeLinkDB.getCubeLinksReferingCube(cube, sessionId);
        if (!HspUtils.isNullOrEmpty(cubeLinks)) {
            Properties p = new Properties();
            p.put("PLAN_TYPE", cube.getPlanTypeName());
            throw new HspRuntimeException("MSG_CUBE_CUBELINK_REFERENCE", p);
        }
    }

    public synchronized void deleteCube(HspCube cube, int sessionId) throws Exception {
        HspUtils.verifyArgumentNotNull(cube, "Cube");
        hspStateMgr.verify(sessionId);
        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));

        // Check if we have any referencing objects
        checkForReferencingObjects(cube, sessionId);

        HspActionSet actionSet = new HspActionSet(hspSQL, user);

        // Remove any dimension associations
        disassociateDimensions(actionSet, cube.getPlanType(), sessionId);

        // update system config for newly added plan type
        HspSystemCfg sysCfg = hspJS.getSystemCfg();
        sysCfg = (HspSystemCfg)sysCfg.cloneForUpdate();
        sysCfg.setPlanTypes(sysCfg.getPlanTypes() & (~cube.getPlanType()));


        HspCubeAction cubeAction = new HspCubeAction();
        actionSet.addAction(cubeAction, HspActionSet.DELETE, cube);
        actionSet.addAction(new HspSystemCfgAction(), HspActionSet.UPDATE, sysCfg);
        actionSet.doActions();
    }
    /*
    public synchronized void addEssbaseCube(HspEssbaseCube newCube, int sessionId) throws Exception {
       hspStateMgr.verify(sessionId);
       validateCube(newCube);
       HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
       HspActionSet actionSet = new HspActionSet(hspSQL, user);
       HspEssbaseCubeAction cubeAction = new HspEssbaseCubeAction();
       actionSet.addAction(cubeAction, HspActionSet.ADD, newCube);
       actionSet.doActions();
    }

    public synchronized void deleteEssbaseCube(HspEssbaseCube newCube, int sessionId) throws Exception {
       hspStateMgr.verify(sessionId);
       HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
       HspActionSet actionSet = new HspActionSet(hspSQL, user);
       HspEssbaseCubeAction cubeAction = new HspEssbaseCubeAction();
       actionSet.addAction(cubeAction, HspActionSet.DELETE, newCube);
       actionSet.doActions();
    }
*/

    public ModuleMemberAdapter getModuleMemberAdapter(int sessionId) {
        HspCube cube = null;
        int workforcePlanType = HspConstants.PLAN_TYPE_NONE;
        int capExPlanType = HspConstants.PLAN_TYPE_NONE;
        int projectPlanType = HspConstants.PLAN_TYPE_NONE;
        int HSFPlanType = HspConstants.PLAN_TYPE_NONE;
        int finstmtPlanType = HspConstants.PLAN_TYPE_NONE;
        int wfscPlanType = HspConstants.PLAN_TYPE_NONE;
        int epbcsrepPlanType = HspConstants.PLAN_TYPE_NONE;
        Vector<HspCube> cubes = getCubes(sessionId);
        if (cubes != null)
            for (int i = 0; i < cubes.size(); i++) {
                cube = cubes.get(i);
                if (cube.getType() == HspConstants.WORKFORCE_CUBE)
                    workforcePlanType = cube.getPlanType();
                if (cube.getType() == HspConstants.CAPEX_CUBE)
                    capExPlanType = cube.getPlanType();
                if (cube.getType() == HspConstants.PROJECT_CUBE)
                    projectPlanType = cube.getPlanType();
                if (cube.getType() == HspConstants.HSF_CUBE)
                    HSFPlanType = cube.getPlanType();
                if (cube.getType() == HspConstants.FINSTMT_CUBE)
                    finstmtPlanType = cube.getPlanType();
                if (cube.getType() == HspConstants.WFSC_CUBE)
                    wfscPlanType = cube.getPlanType();
                if (cube.getType() == HspConstants.EPBCSREP_CUBE && cube.getObjectName().equals(HspConstants.EPBCSREP_CUBE_NAME))
                    epbcsrepPlanType = cube.getPlanType();


            }
        return new ModuleMemberAdapter(workforcePlanType, capExPlanType, projectPlanType, HSFPlanType, finstmtPlanType, wfscPlanType, epbcsrepPlanType);
    }

    public ModuleDimensionAdapter getModuleDimensionAdapter(int sessionId) {
        HspCube cube = null;
        int workforcePlanType = HspConstants.PLAN_TYPE_NONE;
        int capExPlanType = HspConstants.PLAN_TYPE_NONE;
        int projectPlanType = HspConstants.PLAN_TYPE_NONE;
        int HSFPlanType = HspConstants.PLAN_TYPE_NONE;
        int finstmtPlanType = HspConstants.PLAN_TYPE_NONE;
        int wfscPlanType = HspConstants.PLAN_TYPE_NONE;
        int epbcsrepPlanType = HspConstants.PLAN_TYPE_NONE;
        Vector<HspCube> cubes = getCubes(sessionId);
        if (cubes != null)
            for (int i = 0; i < cubes.size(); i++) {
                cube = cubes.get(i);
                if (cube.getType() == HspConstants.WORKFORCE_CUBE)
                    workforcePlanType = cube.getPlanType();
                if (cube.getType() == HspConstants.CAPEX_CUBE)
                    capExPlanType = cube.getPlanType();
                if (cube.getType() == HspConstants.PROJECT_CUBE)
                    projectPlanType = cube.getPlanType();
                if (cube.getType() == HspConstants.HSF_CUBE)
                    HSFPlanType = cube.getPlanType();
                if (cube.getType() == HspConstants.FINSTMT_CUBE)
                    finstmtPlanType = cube.getPlanType();
                if (cube.getType() == HspConstants.WFSC_CUBE)
                    wfscPlanType = cube.getPlanType();
                if (cube.getType() == HspConstants.EPBCSREP_CUBE && cube.getObjectName().equals(HspConstants.EPBCSREP_CUBE_NAME))
                    epbcsrepPlanType = cube.getPlanType();
            }
        return new ModuleDimensionAdapter(workforcePlanType, capExPlanType, projectPlanType, HSFPlanType, finstmtPlanType, wfscPlanType, epbcsrepPlanType);
    }

    public boolean isDimensionSecured(int dimId, int sessionId) {
        //Do not verify here due to performance impact.
        //hspStateMgr.verify(sessionId);
        HspDimension dim = getDimRoot(dimId);
        if (dim == null) {
            InvalidDimensionException x = new InvalidDimensionException(Integer.toString(dimId));
            logger.throwing(x);
            throw x;
        }
        return dim.getEnforceSecurity();
    }


    ///////////////////////////////////////////////////////////
    // BEGIN New Methods for DEDB which make HAL work
    ////////////////////////////////////////////////////////////

    private HspAction createAction(HspMember member) {
        return createAction(member, null);
    }

    private HspAction createAction(HspMember member, HspActionSet actionSet) {
        Validate.notNull(member, "member is null.");
        // make sure object type checks uses this as opposed to getObjectType - necessitated for shared members
        int objectType;
        if (member.isSharedMember()) {
            HspMember baseMember = getBaseMemberOrFailIfExpected(member, actionSet);
            objectType = baseMember.getObjectType();
        } else
            objectType = member.getObjectType();
        return createAction(objectType);
    }

    private HspAction createAction(int objectType) {
        switch (objectType) {
        case HspConstants.kDimensionScenario:
            return new HspScenarioAction();
        case HspConstants.kDimensionVersion:
            return new HspVersionAction();
        case HspConstants.kDimensionEntity:
            return new HspEntityAction();
        case HspConstants.kDimensionAccount:
            return new HspAccountAction();
        case HspConstants.kDimensionTimePeriod:
            return new HspTimePeriodAction();
        case HspConstants.kDimensionYear:
            return new HspYearAction();
        case HspConstants.kDimensionCurrency:
            return new HspCurrencyAction();
        case HspConstants.gObjType_Currency:
            return new HspCurrencyAction();
        case HspConstants.gObjType_AttributeDim:
            return new HspAttributeDimensionAction();
        case HspConstants.gObjType_AttributeMember:
            return new HspAttributeMemberAction();
        case HspConstants.gObjType_DPDimension:
            return new HspDPDimensionAction();
        case HspConstants.gObjType_DPDimMember:
            return new HspDPMemberAction();
            //                case HspConstants.gObjType_SimpleCurrency:
            //		    return new HspCurrencyAction();
        case HspConstants.gObjType_UserDefinedMember:
        case HspConstants.gObjType_BudgetRequest:
        case HspConstants.gObjType_DecisionPackageASO:
        case HspConstants.gObjType_BudgetRequestASO:

            //case HspConstants.gObjType_BRDimMember:
            return new HspMemberAction();
        case HspConstants.gObjType_Dimension:
            return new HspDimensionAction();
        case HspConstants.gObjType_ReplacementDimension:
            return new HspReplacementDimensionAction();
        case HspConstants.gObjType_ReplacementMember:
            return new HspReplacementMemberAction();
        case HspConstants.gObjType_Metric:
            return new HspMetricAction();
        default:
            throw new IllegalArgumentException("Invalid Object Type: " + objectType);
        }
    }

    public void lockObject(HspObject object, int sessionId) throws Exception {
        if (logger.isLogFinestEnabled())
            logger.entering(object, sessionId);
        HspUtils.verifyArgumentNotNull(object, "object");
        HspSession session = hspStateMgr.getSession(sessionId);
        HspUser user = hspSecDB.getUser(session.getUserId());
        if (user == null) {
            IllegalStateException x =
                new IllegalStateException("[" + hspJS.getAppName() + "] Unrecognized userId: " + session.getUserId() + " found in session: [" + session.getSessionId() + ", " + (session.getPrincipal() != null ? session.getPrincipal().getName() : "null") + ", " +
                                          (session.getPrincipal() != null ? session.getPrincipal().getIdentity() : "null"));
            logger.throwing(x);
            throw x;
        } else {
            logger.fine("[" + hspJS.getAppName() + "] Valid userId: " + session.getUserId() + " found in session: [" + session.getSessionId() + ", " + (session.getPrincipal() != null ? session.getPrincipal().getName() : "null") + ", " +
                        (session.getPrincipal() != null ? session.getPrincipal().getIdentity() : "null"));
            // System.out.println("[" + hspJS.getAppName() + "] Valid userId: " + session.getUserId() + " found in session: [" + session.getSessionId() + ", " + (session.getPrincipal() != null ? session.getPrincipal().getName() : "null") +  ", " + (session.getPrincipal() != null ? session.getPrincipal().getIdentity() : "null"));
        }

        //Create and initialize the lock object
        HspLock lock = new HspLock();
        lock.setObjectId(object.getId());
        lock.setSessionId(sessionId);
        lock.setUserId(session.getUserId());
        //If we already have the lock, ignore the lock command
        if (session.hasLock(lock))
            return;
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        HspLockAction action = new HspLockAction();
        actionSet.addAction(action, HspActionSet.ADD, lock);
        try {
            actionSet.doActions();
        } catch (Exception e) {
            ObjectIsLockedException x = new ObjectIsLockedException(object.getName(), null);
            logger.throwing(x);
            throw x;
        }
        //If the lock is aquired in SQL, add it to the session
        session.addLock(lock);
        if (logger.isLogFinestEnabled())
            logger.exiting();
    }

    public void unlockObject(HspObject object, int sessionId) throws Exception {
        HspUtils.verifyArgumentNotNull(object, "object");
        HspSession session = hspStateMgr.getSession(sessionId);
        HspUser user = hspSecDB.getUser(session.getUserId());

        //Create and initialize the lock object
        HspLock lock = new HspLock();
        lock.setObjectId(object.getId());
        lock.setSessionId(sessionId);
        lock.setUserId(session.getUserId());
        //only unlock the dimension in SQL if the user has it locked
        if (session.hasLock(lock)) {
            lock.setUserId(hspStateMgr.getUserId(sessionId));
            HspActionSet actionSet = new HspActionSet(hspSQL, user);
            HspLockAction action = new HspLockAction();
            actionSet.addAction(action, HspActionSet.DELETE, lock);
            actionSet.doActions();
            //If the release is successful, then remove the lock from the session object
            session.removeLock(lock);
        }
    }

    public HspLock getLockStatus(HspObject object, int sessionId) throws Exception {
        HspUtils.verifyArgumentNotNull(object, "object");
        hspStateMgr.verify(sessionId);
        Vector<HspLock> locks = objectLockCacheLoader.loadObjects(new Object[] { object.getId() });
        if (locks == null || locks.isEmpty())
            return null;
        else if (locks.size() > 1) {
            IllegalStateException x = new IllegalStateException("Multiple locks exist on the same object."); // This should never happen
            logger.throwing(x);
            throw x;
        }
        return locks.get(0);
    }

    public void lockDimension(String dimName, int sessionId) throws Exception {
        HspDimension dim = getDimRoot(dimName);
        if (dim == null)
            throw new InvalidDimensionException(dimName);


        lockObject(dim, sessionId);
    }

    public void unlockDimension(String dimName, int sessionId) throws Exception {
        HspDimension dim = getDimRoot(dimName);
        if (dim == null)
            throw new InvalidDimensionException(dimName);


        unlockObject(dim, sessionId);
    }

    public void unlockMyLocks(int sessionId) throws Exception {
        HspSession session = hspStateMgr.getSession(sessionId);
        HspUser user = hspSecDB.getUser(session.getUserId());
        HspLock[] locks = session.getLocks();
        if ((locks == null) || (locks.length <= 0))
            return;
        //Create and initialize the lock object
        HspLock lock = new HspLock();
        lock.setObjectId(HspLock.ALL_OBJECTS);
        lock.setSessionId(sessionId);
        lock.setUserId(session.getUserId());
        //Release the locks
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        HspLockAction action = new HspLockAction();
        actionSet.addAction(action, HspActionSet.DELETE, lock);
        actionSet.doActions();
        //If the release is successful, then remove the lock from the session object
        session.removeAllLocks();
    }

    public void unlockAllMyLocks(int sessionId) throws Exception {
        HspSession session = hspStateMgr.getSession(sessionId);
        HspUser user = hspSecDB.getUser(session.getUserId());
        if (user == null)
            throw new IllegalArgumentException("Invalid userId or session");

        if (user.isPlanningAdministrator())
            throw new HspRuntimeException("MSG_GN_NOT_ENOUGH_ACCESS");

        //Create and initialize the lock object
        HspLock lock = new HspLock();
        lock.setObjectId(HspLock.ALL_MY_LOCKS);
        lock.setSessionId(sessionId);
        lock.setUserId(session.getUserId());
        //Release the locks
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        HspLockAction action = new HspLockAction();
        actionSet.addAction(action, HspActionSet.DELETE, lock);
        actionSet.doActions();
        //If the release is successful, then remove the lock from the session object
        session.removeAllLocks();
    }

    public Vector<HspMember> getSharedMembersOfBase(int dimId, int memberId, int sessionId) throws Exception {
        return getMembersCache(dimId).getObjects(memberBaseMemberIdKeyDef, memberBaseMemberIdKeyDef.createKeyFromBaseMemberId(memberId));
    }

    private Vector<HspMember> getSharedMembersOfBase(int dimId, int memberId, HspActionSet actionSet, int sessionId) throws Exception {
        Vector<HspMember> sharedMembers = getSharedMembersOfBase(dimId, memberId, sessionId);
        if (actionSet != null && memberId > 0) {
            Predicate sharedOfBasePredicate = PredicateUtils.andPredicate(PredicateUtils.instanceofPredicate(HspMember.class), HspPredicateUtils.keyDefPredicate(memberBaseMemberIdKeyDef, memberBaseMemberIdKeyDef.createKeyFromBaseMemberId(memberId)));
            List<DualVal<CachedObject, ActionMethod>> objectAndMethodList = actionSet.findCachedObjects(sharedOfBasePredicate, ActionMethod.ADD, ActionMethod.DELETE, ActionMethod.MOVE, ActionMethod.UPDATE, ActionMethod.UPDATE_ADDING_IF_NEEDED);
            if (HspUtils.size(objectAndMethodList) <= 0)
                return sharedMembers;
            KeyDef keyDef = CachedObjectIdKeyDef.CACHED_OBJECT_ID_KEY;
            Map<Object, HspMember> sharedsByIdMap = HspUtils.addToMap(sharedMembers, keyDef, new LinkedHashMap<Object, HspMember>());
            for (DualVal<CachedObject, ActionMethod> objectAndMethod : objectAndMethodList) {
                HspMember sharedMbr = (HspMember)objectAndMethod.getVal1();
                if (objectAndMethod.getVal2() == ActionMethod.DELETE)
                    sharedsByIdMap.remove(keyDef.createKey(sharedMbr));
                else
                    sharedsByIdMap.put(keyDef.createKey(sharedMbr), sharedMbr);
            }
            sharedMembers = new Vector<HspMember>(sharedsByIdMap.values());
        }
        return sharedMembers;
    }

    public static boolean supportsSharedMembers(int dimId) {
        switch (dimId) {
            //case HspConstants.kDimensionTimePeriod: we now allow it for dts and alternate tp members, and don't prevent it on the server side for other tp's
        case HspConstants.kDimensionYear:
            //case HspConstants.kDimensionScenario: for 9.3.1 enable shareds
            //case HspConstants.kDimensionVersion: for 9.3.1 enable shareds
        case HspConstants.kDimensionRates:
            return false;
        }
        return true;
    }

    public boolean supportsAttributes(int dimId) {
        HspDimension dimension = this.getDimRoot(dimId);
        if (dimension == null) {
            InvalidDimensionException x = new InvalidDimensionException(Integer.toString(dimId));
            logger.throwing(x);
            throw x;
        }

        return dimension.supportsAttributes();
    }

    public synchronized void saveTimePeriod(HspTimePeriod timePeriod, int firstChildId, int lastChildId, int sessionId) throws Exception {
        logger.entering(timePeriod, firstChildId, lastChildId, sessionId);
        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        saveTimePeriod(timePeriod, firstChildId, lastChildId, actionSet, sessionId);
        actionSet.doActions();
        logger.exiting();
    }

    private synchronized void saveTimePeriod(HspTimePeriod timePeriod, int firstChildId, int lastChildId, HspActionSet actionSet, int sessionId) throws Exception {
        hspStateMgr.verify(sessionId);
        hspCalDB.saveTimePeriod(timePeriod, firstChildId, lastChildId, actionSet, sessionId);
        //        Object[][] aliases = timePeriod.getAliasesToBeSaved();
        //        timePeriod.setAliasesToBeSaved(null);
        //        int aliasUpdates = updateMemberAliases(actionSet, timePeriod, aliases, true, sessionId);
    }

    //	public synchronized void addTimePeriod(HspTimePeriod timePeriod, int firstChildId, int lastChildId, int sessionId) throws Exception
    //	{
    //        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
    //		HspActionSet actionSet = new HspActionSet(hspSQL, user);
    //		addTimePeriod(timePeriod, firstChildId, lastChildId, actionSet, sessionId);
    //		actionSet.doActions();
    //	}
    //	public synchronized void addTimePeriod(HspTimePeriod timePeriod, int firstChildId, int lastChildId, HspActionSet actionSet, int sessionId) throws Exception
    //	{
    //		hspStateMgr.verify(sessionId);
    //
    //		// The Calendar and Time Period members are special beasts and don't follow the form of regular
    //		// dimensions and dimension members. They will eventually, but for now they're treated special.
    //		// The following call is for adding a new time period only, see saveMember for mod's.
    //		hspCalDB.saveTimePeriod(timePeriod, firstChildId, lastChildId, actionSet, sessionId);
    //	}

    public void saveMembersOnTheFly(HspMember[] members, int sessionId) throws Exception {
        //        if (members == null)
        //            throw new RuntimeException("Null member array argument specified.");
        //
        //        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId)); // verify session id
        //        HspJNIexec olap = hspJS.getHspJNIExec();
        //        HspRestructure restructure = (HspRestructure)olap.getHspRestructure(sessionId);
        //        List failed = new ArrayList();
        //        for (int i = 0; i < members.length; i++)
        //        {
        //            try
        //            {
        //                if (members[i] == null)
        //                    throw new RuntimeException("Null array element.");
        //                saveMember(members[i], sessionId);
        //                restructure.updateMbr(members[i]);
        //            }
        //            catch (Exception e)
        //            {
        //                failed.add(members[i]);
        //            }
        //        }
        //
        //
        //        olap.execRestructureRequest(restructure);
        //
        //        if (failed.size() > 0)
        //            throw new SaveMembersOnTheFlyException(failed);
    }

    public void saveMemberOnTheFly(HspMember member, int sessionId) throws Exception {
        //        if (member == null)
        //            throw new HspRuntimeException("Null member argument specified.");
        //        HspJNIexec olap = hspJS.getHspJNIExec();
        //        saveMember(member, sessionId);
        //        HspRestructure restructure = (HspRestructure)olap.getHspRestructure(sessionId);
        //        restructure.updateMbr(member);
        //        olap.execRestructureRequest(restructure);
    }

    public void deleteMemberOnTheFly(HspMember member, int sessionId) throws Exception {
        //        if (member == null)
        //            throw new HspRuntimeException("Null member argument specified.");
        //        if (member.hasChildren())
        //            throw new HspRuntimeException("Attempted to delete a non-leaf node member");
        //        HspMember dedbMember = (HspMember) member.cloneForUpdate();
        //        HspJNIexec olap = hspJS.getHspJNIExec();
        //        deleteMembers(dedbMember, true, true, sessionId);
        //        HspMember olapMember = (HspMember) member.cloneForUpdate();
        //        olapMember.setObjectId(dedbMember.getId());
        //        HspRestructure restructure = (HspRestructure)olap.getHspRestructure(sessionId);
        //        restructure.deleteMbr(olapMember);
        //        olap.execRestructureRequest(restructure);
    }

    public void saveDPBRMember(HspDPMember member, final int sessionId) throws Exception {

        HspMember oldHspMember = getDimMember(member.getDimId(), member.getId());
        boolean isBRMember = false;
        String oldMemberName = null;
        if (oldHspMember != null)
            oldMemberName = oldHspMember.getObjectName();
        String newMemberName = member.getObjectName();
        saveMember(member, sessionId);

        // This fetch is required to get the correct objectId
        member = (HspDPMember)getDimMember(member.getDimId(), member.getName());
        if (member.getBrDimMemberId() > 0 || member.getMemberType() == HspConstants.BUDGET_REQUEST_DP_MEMBER) {
            isBRMember = true;
        }
        //Save DP_ASO member
        DPBRDefUpdateUtil.createUpdateDPDimMember(this, this.hspCubeLinkDB, oldMemberName, newMemberName, isBRMember, member.getId(), member, sessionId);
    }

    public void saveASOMember(final HspMember member, final DynamicChildStrategy strategy, final int sessionId) throws Exception {
        saveMember(member, strategy, sessionId);
    }

    /**
     * {@inheritDoc}
     *
     * @param member {@inheritDoc}
     * @param sessionId {@inheritDoc}
     * @throws Exception {@inheritDoc}
     */
    public synchronized void saveMember(HspMember member, int sessionId) throws Exception {
        saveMember(member, DynamicChildStrategy.DYNAMIC_IF_AVAILABLE, sessionId);

        //if adding Input currency then add Reporting currency
        if (member.getDimId() == HspConstants.kDimensionSimpleCurrency && member.getParentId() == HspSimpleCurrencyUtils.MEMBER_ID_HSP_INPUT_CURRENCIES) {
            saveReportingCurrency(member, sessionId);
        }

    }

    /**
     * {@inheritDoc}
     *
     * @param members {@inheritDoc}
     * @param sessionId {@inheritDoc}
     * @throws Exception {@inheritDoc}
     */
    public synchronized void saveMembers(HspMember[] members, int sessionId) throws Exception {
        saveMembers(members, DynamicChildStrategy.DYNAMIC_IF_AVAILABLE, true, sessionId);
    }

    public void saveMembers(HspMember[] members, DynamicChildStrategy strategy, int sessionId) throws Exception {
        saveMembers(members, strategy, true, sessionId);
    }

    private synchronized void saveMember(final HspMember member, final DynamicChildStrategy strategy, final int sessionId) throws Exception {
        saveMembers(new HspMember[] { member }, strategy, false, sessionId);
    }

    private void executeMemberCallbackRules(HspMember member, HspMember oldMember, HspActionSet actionSet, String[] scriptNames, int sessionId) {
        executeMemberCallbackRules(member, oldMember, actionSet, null, scriptNames, sessionId);
    }

    private void executeMemberCallbackRules(HspMember member, HspMember oldMember, HspActionSet actionSet, ActionMethod actionMethod, String[] scriptNames, int sessionId) {
        if (scriptNames != null && scriptNames.length > 0) {
            if (oldMember == null)
                oldMember = (HspMember)((HspMember)member).getClonedFrom();

            if (actionMethod == null) {
                actionMethod = ActionMethod.ADD;
                if ((oldMember != null) && (member.getId() > 0))
                    actionMethod = ActionMethod.UPDATE;
            }

            MemberChangedEvent mbrCtx = HspUtils.createMemberChangedEvent(hspJS, member, oldMember, actionMethod, sessionId);
            List<HspFormCalc> calcs = HspUtils.getFormCalcs(scriptNames, hspJS, sessionId);
            for (HspFormCalc calc : calcs) {
                try {
                    PlanningGroovyExecutor.push();
                    PlanningGroovyExecutor.setCurrentMemberContext(mbrCtx);
                    PlanningGroovyExecutor.setHspJSContext(hspJS);
                    PlanningGroovyExecutor.setSessionIdContext(sessionId);
                    PlanningGroovyExecutor.setActionSetContext(actionSet);

                    HspCalcMgrDB cache = hspJS.getCalcMgrDB(sessionId);
                    HspCube cube = getCubeByPlanType(calc.getPlanType());
                    if (cube != null) {
                        HspCalcMgrRule rule = cache.getRule(calc.getCalcName(), cube.getPlanTypeName());
                        //PlanningGroovyExecutor.setPlanTypeContext(cube.getPlanTypeName());
                        if (rule.getBean() instanceof RuleBean && ((RuleBean)rule.getBean()).isGroovy())
                            PlanningGroovyExecutor.execute(rule.getBean().getScript(), calc.getCalcName());
                        else
                            throw new UnsupportedOperationException("Only Groovy member call back rules are supported.");
                        //hspJS.getFMDB(sessionId).runCalcScript(sessionId, calc, null, false);
                    }
                } catch (Exception e) {
                    logger.info("Failure in executeMemberCallbackRules: " + e.getMessage());
                    //PlanningGroovyExecutor.resetContext();
                    if (e instanceof RuntimeException) //TODO: fix error handling
                        throw (RuntimeException)e;
                    else
                        throw new HspCallbackInvocationException(e);
                } finally {
                    //PlanningGroovyExecutor.removeFromContext(PlanningGroovyExecutor.PLAN_TYPE);
                    PlanningGroovyExecutor.removeFromContext(PlanningGroovyExecutor.HSPJS);
                    PlanningGroovyExecutor.removeFromContext(PlanningGroovyExecutor.EVENT);
                    PlanningGroovyExecutor.removeFromContext(PlanningGroovyExecutor.SESSION_ID);
                    PlanningGroovyExecutor.removeFromContext(PlanningGroovyExecutor.HSP_ACTION_SET);
                    PlanningGroovyExecutor.pop();
                    //PlanningGroovyExecutor.resetContext();
                }
            }
        }
    }


    private synchronized void saveMembers(final HspMember[] members, final DynamicChildStrategy strategy, final boolean optimizeForBatch, final int sessionId) throws Exception {
        if (members == null || members.length == 0)
            return;

        final HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        boolean singleMemberSave = members != null && members.length == 1;
        HspMember originalSingleMbr = (singleMemberSave && members[0] != null) ? (HspMember)members[0].cloneForUpdate() : null;

        // Only retry for single member saves of dynamic (member on the fly) member
        int retryCount = (singleMemberSave && isDynamicMember(members[0], sessionId)) ? 10 : 1;
        Exception exception = null;
        for (int i = 0; i < retryCount; i++) {
            exception = null;
            // If we are retrying while adding dynamic member, sleep for a small amount of time to avoid constant collisions
            if (i > 0) {
                try {
                    Thread.sleep((long)(Math.random() * 30));
                } catch (InterruptedException ie) { /* Ignore Interrupt */
                    ;
                }
            }
            HspAction memberAction = null;
            HspActionSet actionSet = new HspActionSet(hspSQL, user);
            // If more than one member is loaded in this actionSet
            // then create a map to store the pending list of
            // children per parent so that the position logic can
            // honor members that have not been committed yet.
            if (members.length > 1)
                actionSet.setParameter(AS_PARAM_CHILDREN_MAP, new HashMap<Integer, Vector>());

            final double originalPosition = members.length == 1 ? members[0].getPosition() : 0;
            try {
                for (int memberIndex = 0; memberIndex < members.length; memberIndex++) {
                    HspMember member = members[memberIndex];
                    if (member == null)
                        continue;

                    if (memberAction == null) {
                        memberAction = createAction(member, actionSet);
                    }
                    if (member.getParent() == null || member.getParentId() != ((HspMember)member.getParent()).getId()) {
                        member.setParent(getMemberById(member.getParentId()));
                    }
                    //If seeded member & the user is not module user, track the changes
                    boolean isObjectSeeded = hspJS.getFeatureDB(sessionId).isObjectSeeded(member.getId(), member.getObjectType());
                    boolean shouldTrack = HspModuleInfo.getModuleUser() == null || (!HspModuleInfo.getModuleUser().equals(HspModuleInfo.MODULE_USER));
                    List<HspDiff> diffList = null;
                    if (isObjectSeeded && shouldTrack) {
                        ReadOnlyCachedObject objInCache = hspJS.getFeatureDB(sessionId).getPlanningObject(member.getObjectName(), member.getObjectType(), sessionId, member.getParentId());
                        diffList = member.getDiffList(hspJS, objInCache, sessionId);

                        //Below condition is satisfied for Shared Members for which UsedIn has been changed i.e. diffList will NOT be EMPTY
                        if (member.isSharedMember() && diffList != null && !diffList.isEmpty()) {
                            HspDiffUtil.addParameterToDiffList("PARENT_NAME", member.getParent().getName(), diffList);
                        }
                    }
                    // If the position changed, and the position was not a negative value
                    // such as first or last sibling, then update the position of all future
                    // members to compensate for the change in position.
                    double positionBeforeSave = member.getPosition();
                    saveMember(member, actionSet, strategy, sessionId);
                    if (positionBeforeSave > 0 && Double.compare(positionBeforeSave, member.getPosition()) != 0) {
                        for (int futureMemberIndex = memberIndex + 1; futureMemberIndex < members.length; futureMemberIndex++) {
                            HspMember futureMember = members[futureMemberIndex];
                            // Check the positions of all future siblings.
                            // A future member is a future sibling if the
                            // parentIds are the same.
                            if (futureMember != null && futureMember.getParentId() == member.getParentId()) {
                                // If a member's position changed, update all other members with the same
                                // oldPosition to the same newPosition
                                if (Double.compare(futureMember.getPosition(), positionBeforeSave) == 0)
                                    futureMember.setPosition(member.getPosition());
                                // If the member was moved in such a way as it is no longer compatible with members in the
                                // array, then throw an exception and let batch retry with a smaller set.
                                else if (Double.compare(futureMember.getPosition(), positionBeforeSave) != Double.compare(futureMember.getPosition(), member.getPosition()))
                                    throw new RuntimeException("Batch size could cause members to load out of order, try again with a smaller batch size.");
                            }
                        }
                    }
                    if (isObjectSeeded && shouldTrack && diffList != null && diffList.size() > 0) {
                        int artifactId = hspJS.getFeatureDB(sessionId).getModuleArtifactDetailByObjId(member.getId(), member.getObjectType()).getId();
                        // TODO: Verify the next line handles the rollback of the actionSet as this method maybe retried multiple times
                        hspJS.getFeatureDB(sessionId).addAuditRecord(HspActionSet.UPDATE, diffList, artifactId, sessionId);
                    }
                }
                // Optimize the actionSet for batch if requested and
                // the memberAction is not null
                if (optimizeForBatch && memberAction != null)
                    actionSet.optimizeForBatch(false, new DualVal(HspMemberOnFlyCompositeAction.class, ActionMethod.ADD), new DualVal(memberAction.getClass(), ActionMethod.ADD), new DualVal(memberAction.getClass(), ActionMethod.UPDATE));
                actionSet.doActions();
                break;
            } catch (HspDynamicChildrenExhaustedException e) {
                // The idea here is to use the buckets if available for dynamic children, but go ahead and save as normal members
                // depending on the strategy. When dynamic children get saved as normal members, they will not be usable until the next
                // cube refresh is run.
                if (strategy == DynamicChildStrategy.ALWAYS_DYNAMIC || strategy == DynamicChildStrategy.ALWAYS_DYNAMIC_IF_ENABLED)
                    throw e;

                // If more than one member is being saved always throw the exception as we do not know which member failed.
                if (!singleMemberSave) {
                    throw e;
                } else {
                    members[0].setPosition(originalPosition);
                    members[0].setMemberOnFlyDetailToBeSaved(null);
                    saveMember(members[0], actionSet, DynamicChildStrategy.NEVER_DYNAMIC, sessionId);

                    // Optimize the actionSet for batch if requested and the memberAction is not null
                    if (optimizeForBatch && memberAction != null)
                        actionSet.optimizeForBatch(false, new DualVal(HspMemberOnFlyCompositeAction.class, ActionMethod.ADD), new DualVal(memberAction.getClass(), ActionMethod.ADD), new DualVal(memberAction.getClass(), ActionMethod.UPDATE));
                    actionSet.doActions();
                    break;
                }
            } catch (Exception e) {
                exception = e;
                if (singleMemberSave && members[0] != null && !members[0].isReadOnly()) {
                    members[0].setAttributesToBeSaved(originalSingleMbr.getAttributesToBeSaved());
                    members[0].setUDAsToBeSaved(originalSingleMbr.getUDAsToBeSaved());
                    members[0].setAliasesToBeSaved(originalSingleMbr.getAliasesToBeSaved());
                    members[0].setMemberOnFlyDetailToBeSaved(originalSingleMbr.getMemberOnFlyDetailToBeSaved());
                    members[0].setMemberPTPropsToBeSaved(originalSingleMbr.getMemberPTPropsToBeSaved());
                }
            }
        }
        if (exception != null)
            throw exception;
    }

    public synchronized void saveMember(HspMember member, HspActionSet actionSet, int sessionId) throws Exception {
        saveMember(member, actionSet, DynamicChildStrategy.DYNAMIC_IF_AVAILABLE, sessionId);
    }

    /**
     * Returns the baseMember for this member if it is a shared member,
     * otherwise it returns null.
     * <p>
     * If the member is a shared member, this method will never return null.
     * Instead, it will either return the base member or throw an
     * IllegalStateException.
     * <p>
     * This method will always return null if the member is* null or not a
     * shared member.
     *
     * @param member a valid member or null
     * @param actionSet a valid action set or null
     * @return the baseMember for this member if it is a shared member,
     * otherwise it returns null.
     * @throws IllegalStateException if the member is a shared member and the
     *         base member cannot be located
     */
    private HspMember getBaseMemberOrFailIfExpected(HspMember member, HspActionSet actionSet) {
        HspMember baseMember = getBaseMember(member, actionSet);
        if (member != null && member.isSharedMember() && baseMember == null) {
            //HspMember tempBase = (getDimMember(dim.getId(), member.getBaseMemberId(), actionSet));
            if (baseMember == null) {
                HspRuntimeException x = new HspRuntimeException("MSG_UNABLE_TO_LOCATE_BASE_MEMBER", new RuntimeException("Unable to add shared member " + member.getObjectName() + " because base member does not exist."));
                logger.throwing(x);
                throw x;
            }
        }
        return baseMember;
    }

    private HspMember getBaseMember(HspMember member, HspActionSet actionSet) {
        if (member == null || !member.isSharedMember())
            return null;

        if (actionSet != null) {
            // First try by member id and return if found.
            HspMember baseMember = getDimMember(member.getDimId(), member.getBaseMemberId(), actionSet);
            if (baseMember != null)
                return baseMember;
            // Then walk the transaction to find the lastest copy of the base
            Predicate baseMemberPredicate = PredicateUtils.andPredicate(PredicateUtils.instanceofPredicate(HspMember.class), HspPredicateUtils.sharedMemberPredicate(false));
            Predicate baseMemberWithNamePredicate = PredicateUtils.andPredicate(baseMemberPredicate, HspPredicateUtils.memberNamePredicate(member.getMemberName()));

            List<DualVal<CachedObject, ActionMethod>> objectAndMethodList = actionSet.findCachedObjects(baseMemberWithNamePredicate, ActionMethod.ADD, ActionMethod.DELETE, ActionMethod.MOVE, ActionMethod.UPDATE, ActionMethod.UPDATE_ADDING_IF_NEEDED);
            // If the list is empty, return null as no matching base was found
            // If the last item in the list is delete, return null as the base
            // member was deleted as the last reference in this transaction.
            DualVal<CachedObject, ActionMethod> lastEntry = objectAndMethodList.isEmpty() ? null : objectAndMethodList.get(objectAndMethodList.size() - 1);
            if (lastEntry == null || lastEntry.getVal2() == ActionMethod.DELETE)
                return null;
            return (HspMember)lastEntry.getVal1();
        }
        return getDimMember(member.getDimId(), member.getBaseMemberId());
    }

    public synchronized void saveMember(HspMember member, HspActionSet actionSet, DynamicChildStrategy strategy, int sessionId) throws Exception {
        updateCurrencyUDAForMember(hspJS, member, sessionId);
        // Invoke Pre Processing Groovy rules
        String[] scriptNames = PBCSPreAndPostScriptFactory.getInstance().getScriptNames(hspJS.getSystemCfg().getApplicationType(), HspPreAndPostOperationType.PRE_MEMBER_SAVE);
        if (!HspUtils.isNullOrEmpty(scriptNames)) {
            HspMember oldMember = getDimMember(member.getDimId(), member.getId(), actionSet);
            executeMemberCallbackRules(member, oldMember, actionSet, scriptNames, sessionId);
        }
        HspMemberInfo memberInfo = saveMemberBypassCallbacks(member, actionSet, strategy, sessionId);
        executeMemberCallbackRules((memberInfo != null ? memberInfo.getMember() : member), (memberInfo != null ? memberInfo.getOldMember() : null), actionSet,
                                   PBCSPreAndPostScriptFactory.getInstance().getScriptNames(hspJS.getSystemCfg().getApplicationType(), HspPreAndPostOperationType.POST_MEMBER_QUEUE_FOR_SAVE), sessionId);
    }

    private synchronized HspMemberInfo saveMemberBypassCallbacks(HspMember member, HspActionSet actionSet, DynamicChildStrategy strategy, int sessionId) throws Exception {
        HspMemberInfo memberInfo = null;
        try {
             if ((oldMember != null) && (member.getId() > 0)) // if we're doing an update only
            {
                memberInfo = new HspMemberInfo(member, oldMember, sessionId);
                doUpdate = true;
                // Attributes cannot be associated with Shared Members only their base members, nor can attributes be
                // associated with label-only members. For label-only members let attributes be deleted (a zero-length
                // attributesToBeSaved array. This supports the case of changing an existing member from non label-only to
                // label-only, deleting attribute assignments in the process.

                //  throw an error if this is shared or labelOnly and attributes are specified for saving
                if ((member.isSharedMember() || member.hasLabelOnlyDataStorage()) && member.getAttributesToBeSaved() != null && member.getAttributesToBeSaved().length != 0 && member.getShouldSaveAttributes() && containsIndexedAttributes(member.getAttributesToBeSaved()))
                    throw new HspRuntimeException("MSG_NO_ATTRIBUTES_ON_SHARED_OR_LABEL_ONLY");

                // delete existing attributes if this is shared or labelOnly and shouldSave was false
                if ((member.isSharedMember() || member.hasLabelOnlyDataStorage()) && !member.getShouldSaveAttributes()) {
                    HspAttributeMemberBinding[] noBindings = { };
                    member.setAttributesToBeSaved(noBindings);
                }

                // if there's no need to change the member, check aliases and attributes to see if they should be saved
                // if the member properties are unchanged don't bother to add actions for them, just update the timestamp
                // if (member.propertiesEquivalentTo(oldMember))
                // for now always save attributes and aliases
                int attributeUpdates = 0;
                int aliasUpdates = 0;
                int udaUpdates = 0;

                if ((dim.getObjectType() != HspConstants.gObjType_AttributeDim) && (dim.getObjectType() != HspConstants.gObjType_AttributeMember)) {
                    // Throw an error if a member is enabled for dynamic children and attributes are being associated with it. Attributes have to be
                    // associated at the same level for all members within a dimension. But when a member is enabled for dynamic children, the level
                    // for that member will change during cube create/refresh as dynamic children buckets get added and an error will occur at that
                    // point if not handled here.
                    if (member.getAttributesToBeSaved() != null && member.getAttributesToBeSaved().length > 0 && member.getShouldSaveAttributes() && member.getMemberOnFlyDetailToBeSaved() != null)
                        throw new HspRuntimeException("MSG_NO_ATTRIBUTES_ON_DYN_PARENT_MEMBERS");

                    HspAttributeMemberBinding[] attributeBindings = member.getAttributesToBeSaved();
                    member.setAttributesToBeSaved(null);
                    Object[][] aliases = member.getAliasesToBeSaved();
                    member.setAliasesToBeSaved(null);
                    HspUDABinding[] udaBindings = member.getUDAsToBeSaved();
                    member.setUDAsToBeSaved(null);
                    HspMemberOnFlyDetail mbrOnFlyDetail = member.getMemberOnFlyDetailToBeSaved();
                    member.setMemberOnFlyDetailToBeSaved(null);

                    attributeUpdates = updateMemberAttributeBindings(actionSet, member.getId(), member.getDimId(), attributeBindings, memberInfo);
                    aliasUpdates = updateMemberAliases(actionSet, member, aliases, true, memberInfo, sessionId);
                    udaUpdates = updateMemberUDABindings(actionSet, member.getId(), member.getDimId(), udaBindings, memberInfo);
                    updateMemberOnTheFlyDetail(actionSet, member, mbrOnFlyDetail, memberInfo, sessionId);

                    // The old cube refresh examined time stamps. For the new cube refresh we no longer need to do updates
                    // so the changes get picked up - an outline change is sufficient.
                    //                     Smart-save  Don't bother fooling around with member properties if none have changed, so just return
                    //                     If attributes or aliases changed associated actions will be in the action set at this point.
                    if (member.propertiesEquivalentTo(oldMember)) {
                        // update the timestamp (for cube refresh) if attributes or aliases changed
                        if (attributeUpdates > 0 || aliasUpdates > 0 || udaUpdates > 0) {
                            HspTimestampAction timestampAction = new HspTimestampAction();
                            actionSet.addAction(timestampAction, HspActionSet.UPDATE, member);
                        }
                        actionSet.addAction(new HspMemberInfoAction(), HspActionSet.UPDATE, memberInfo);
                        return memberInfo;
                    }
                }
            }

            if (member.getDataStorage() == HspConstants.kDataStorageSharedMember || member.isSharedMember()) {
                if (baseMember == null)
                    throw new HspRuntimeException("MSG_UNABLE_TO_LOCATE_BASE_MEMBER", new RuntimeException("Unable to add shared member " + member.getObjectName() + " because base member does not exist."));

                if (!supportsSharedMembers(member.getDimId()))
                    throw new IllegalArgumentException("Shared Members not supported for Dimension: " + dim.getName());

                // Edge case: can't change a base to a shared
                if (member.getId() > 0 && member.getId() == baseMember.getId())
                    throw new HspRuntimeException("MSG_BASE_MEMBER_CANNOT_BE_CHANGED_TO_SHARED", new RuntimeException("BaseId of shared member: " + member.getObjectName() + " is equal to id of member - self reference."));

                HspMember oldShared = getDimMember(member.getDimId(), member.getId(), actionSet);
                if ((oldShared != null) && (member.propertiesEquivalentTo(oldShared)))
                    return memberInfo;

                member.setObjectType(HspConstants.gObjType_SharedMember);
                // shared members cant have children
                member.setHasChildren(false);
                // Set usedIn so cube refresh sticks the shared where it should be
                // entity may override this below
                // Do we wanna AND member.getUsedIn() since it requires user to set this property before save?
                member.setUsedIn(member.getUsedIn() & getUsedIn(parent) & baseMember.getUsedIn());

                // Since this is a shared member set the description to the base's
                member.setDescription(baseMember.getDescription());

            }
            /////////////////////////////////////////////////////////////////////////////////////////
            // Common Object-level Checks (done for all three: accounts; entities; and user defined, all the same way)
            // validate the dimension member name.
            if (member.getObjectName() != null)
                member.setObjectName(member.getObjectName().trim()); // trim leading & trailing white space
            validateDimensionMemberName(member.getObjectName()); // validate name follows rules...
            // validate the aliases on the member
            //validateAliases(member);

            //            if (hspAlsDB.isAliasNameInUse(member.getMemberName()))
            //            {
            //                Properties p = new Properties();
            //                p.put("DIMENSION_MEMBER_NAME", member.getMemberName());
            //                throw new HspRuntimeException("MSG_NAME_IN_USE_AS_ALIAS", p, new RuntimeException("The name \""+member.getMemberName()+"\" is being used as an alias name."));
            //            }

            // validate the description field. no trimming done: responisiblity of the callers
            validateObjectDescription(member.getDescription());

            if (isNotMemberObjectType(objectType)) // check object type
                throw new RuntimeException("Invalid Object Type (" + objectType + ") for member name: " + member.getName());


            member.setGeneration(parent.getGeneration() + 1); // Set the generation value

            // Attributes cannot be associated with Shared Members only their base members, nor can attributes be
            // associated with label-only members. For label-only members let attributes be deleted (a zero-length
            // attributesToBeSaved array. This supports the case of changing an existing member from non label-only to
            // label-only, deleting attribute assignments in the process.
            // This test was also performed above in smart-save logic for updates
            validateAttributeAssignmentAgainstDataStorage(member);
            /////////////////////////////////////////////////////////////////////////////////////////
            // Common Member-level Checks
            // member.setDimId   validated above
            for (int plan = 1; plan <= HspConstants.PLAN_TYPE_ALL; plan = plan << 1) {
                //                if (member.getParentId() == member.getDimId())
                //                    checkHierarchyTypeForASO(member, plan);

                validateConsolidationOperator(member.getConsolOp(plan), plan);
                validateConsolidationOperatorForASO(member, plan);

            }
            // member.isUsedForConsol(); //dont need to check this cause it's boolean now.
            validateDataStorage(member);
            //member.twopassCalc; don't need twopasscalc check cause its a boolean now.
            validateDataStorageWith2PassSetting(member);
            validatePlanTypesAndSourcePlanType(member, actionSet);
            validateFlatDimensionMembersParent(member);
            validateEnumerationId(member);
            validateDataType(member);
            // Member formula should not be validated on save as Essbase and
            // Planning allow the user to save invalid member formulas.
            //////////////////////////////////////////////////////////////////////////////////
            // member type-specific checks
            switch (dim.getObjectType()) {
            case HspConstants.gObjType_Account: // accounts
                HspAccount account = (HspAccount)member;
                validateAndSetAccountMemberInformation(account, actionSet);
                break;

            case HspConstants.gObjType_Entity:
                HspEntity entity = (HspEntity)member;
                int curId = entity.getDefaultCurrency();
                HspCurrency currency;
                // requisitionNumber and employeeId must be null/0-length if entity is not an employee
                if ((entity.getEntityType() != HspConstants.kEntityTypeEmployee) && (((entity.getRequisitionNumber() != null) && (entity.getRequisitionNumber().length() > 0)) || ((entity.getEmployeeId() != null) && (entity.getEmployeeId().length() > 0))))
                    throw new RuntimeException("Requisition Number and Employee Id must be null or zero length if Entity Type is not Employee.");


                //
                //                    if (entity.getEntityType() == HspConstants.kEntityTypeEmployee)
                //                    {
                //                        if (entity.isEnabledForPM())
                //                            throw new HspRuntimeException("MSG_PROCESS_MGMT_CANNOT_BE_ENABLED_FOR_EMPLOYEES");
                //                        if ((entity.getUsedIn() & HspConstants.kCubeTypeAllValid) != HspConstants.kCubeTypeWorkforce)
                //                            throw new HspRuntimeException("MSG_EMPLOYEE_NOT_IN_WF_PLAN_TYPE_ONLY");
                //
                //                    }

                if (entity.isSharedMember()) {
                    // for shared entity members, the defualt currency is the Base Member's default currency.
                    HspEntity baseEntity = (HspEntity)baseMember;
                    entity.setDefaultCurrency(baseEntity.getDefaultCurrency());
                    entity.setEntityType(baseEntity.getEntityType());
                    entity.setEmployeeId(baseEntity.getEmployeeId());
                    entity.setRequisitionNumber(baseEntity.getRequisitionNumber());
                } else if (curId == 0) {
                    currency = hspCurDB.getDefaultCurrency(sessionId).firstElement();
                    curId = currency.getId();
                    entity.setDefaultCurrency(curId);
                } else {
                    currency = hspCurDB.getCurrency(curId);
                    if (currency == null)
                        throw new RuntimeException("Invalid null defalult currency for member " + member.getName() + " id: " + member.getId());
                }
                if (entity.isSharedMember()) {
                    HspEntity baseEntity = (HspEntity)baseMember;
                    entity.setUsedIn(entity.getUsedIn() & getUsedIn(parent) & baseEntity.getUsedIn());
                } else {
                    // Ensure used in plan types is set to something valid
                    int memberUsedIn = entity.getUsedIn();
                    entity.setUsedIn(deriveValidPlanType(memberUsedIn, parent.getUsedIn()));
                }
                HspEntity entityParent = (HspEntity)parent;
                if ((entityParent.getEntityType() == HspConstants.kEntityTypeEmployee) || (entityParent.getEntityType() == HspConstants.kEntityTypeDepartmentGeneral) || (entityParent.getEntityType() == HspConstants.kEntityTypeTBHInput))
                    throw new RuntimeException("Invalid parent id: parent cannot be of type employee, department general, or TBH input.");
                break;

            case HspConstants.gObjType_AttributeDim:
            case HspConstants.gObjType_AttributeMember:
                // should not be able to get here but throw exception in case we do in the future
                throw new RuntimeException("Attribute members should have been handled already.");


                //TODO: fix attribute member
                //break;
            case HspConstants.gObjType_UserDefinedMember:
                // make sure used in is set correctly
                if (member.isSharedMember()) {
                    member.setUsedIn(member.getUsedIn() & getUsedIn(parent) & baseMember.getUsedIn());
                } else {
                    int memberUsedIn = member.getUsedIn();
                    member.setUsedIn(deriveValidPlanType(memberUsedIn, parent.getUsedIn()));
                }

                break;
            case HspConstants.gObjType_Scenario:
                HspScenario scenario = (HspScenario)member;
                if (!isValidScenario(scenario, sessionId)) {
                    throw new RuntimeException("Cannot Save scenario - Validate Scenario failed" + scenario.getId());
                }

                if ((!scenario.isEnabledForPM()) && isReferencedByPlanningUnits(scenario, sessionId)) {
                    Properties p = new Properties();
                    p.put("MEMBER_NAME", scenario.getName());
                    throw new HspRuntimeException("MSG_MEMBER_CANNOT_BE_DISABLED_FOR_PM", p);
                }
                //validateFlatDimensionMembersParent(scenario);
                break;
            case HspConstants.gObjType_Version:
                HspVersion version = (HspVersion)member;
                if (!isValidVersion(version, sessionId))
                    throw new RuntimeException("Cannot Save version - Validate version failed" + version.getId());

                if ((!version.isEnabledForPM()) && isReferencedByPlanningUnits(version, sessionId)) {
                    Properties p = new Properties();
                    p.put("MEMBER_NAME", version.getName());
                    HspRuntimeException x = new HspRuntimeException("MSG_MEMBER_CANNOT_BE_DISABLED_FOR_PM", p);
                    logger.throwing(x);
                    throw x;
                }
                if ((version.isEnabledForPM()) && (version.getVersionType() == HspConstants.VERSION_OFFICIAL_TARGET)) {
                    HspRuntimeException x = new HspRuntimeException("MSG_TARGET_VERSIONS_DONT_SUPPORT_PM");
                    logger.throwing(x);
                    throw x;
                }
                // remove this check so parent can participate in process management
                //                if (((HspVersion)parent).getVersionType() == HspConstants.VERSION_OFFICIAL_BU)
                //                {
                //                      Properties p = new Properties();
                //                      p.put("MEMBER_NAME", version.getName());
                //                      throw new HspRuntimeException("MSG_NO_KIDS_ALLOWED_BOTTOMUP_VERSION", p);
                //                  }
                //validateFlatDimensionMembersParent(version);
                break;
            case HspConstants.gObjType_SimpleCurrency:
            case HspConstants.gObjType_Currency:
                HspCurrency thiscurrency = (HspCurrency)member;
                if (!(thiscurrency.isLocalCurrency() || thiscurrency.isSystemCurrency())) // don't validate Local currency, it breaks bpma deployment,don't validate System Currencies as they dont have symbol,scale,thousand separator etc.
                    validateCurrency(thiscurrency, sessionId);
                validateFlatDimensionMembersParent(thiscurrency);
                //if adding Input currency then add Reporting currency
                if (member.getParentId() == HspSimpleCurrencyUtils.MEMBER_ID_HSP_INPUT_CURRENCIES) {
                    saveReportingCurrency(member, sessionId);
                }
                break;
            case HspConstants.gObjType_Year:
                HspYear year = (HspYear)member;
                validateYear(year);
                validateFlatDimensionMembersParent(year);
                break;
            case HspConstants.gObjType_Period:
                HspTimePeriod timePeriod = (HspTimePeriod)member;
                validateTimePeriod(timePeriod, parent);
                break;
            case HspConstants.gObjType_ReplacementMember:
                HspReplacementMember replacementMember = (HspReplacementMember)member;
                validateReplacementMember(replacementMember);
                break;
            case HspConstants.gObjType_Metric:
                HspMetric metric = (HspMetric)member;
                validateMetric(metric);
                break;
            case HspConstants.gObjType_DPDimMember:
                if (hasDuplicateMemberName(member, doUpdate)) {
                    Properties p = new Properties();
                    p.put("OBJECT_NAME", member.getObjectName());
                    HspRuntimeException x = new HspRuntimeException("MSG_SQL_DUPLICATE_OBJECT", p);
                    logger.throwing(x);
                    throw x;
                }
                break;
            case HspConstants.gObjType_BudgetRequest:
            case HspConstants.gObjType_DecisionPackageASO:
            case HspConstants.gObjType_BudgetRequestASO:
                //case HspConstants.gObjType_BRDimMember:
                break;
            default:
                throw new RuntimeException("Invalid Member Type for ID: " + member.getId());
            }

            // pick up changes to member (possibly) made in position setting
            // make sure object types match in case we have a base/shared member match
            HspMember tempMember = (HspMember)actionSet.getCachedObject(member);
            if (tempMember != null && tempMember != member && inBatch)
                throw new RuntimeException("Conflicting actions detected on member [" + member == null ? null : member.getNameEx() + "] during batch, repeat with a smaller batch size.");

            setUniqueName(member, actionSet);

            if (doUpdate) {
                // The member being update is being enabled for PM, check if it will exceed the threshold for PM enabled scenarios.
                if (member.isEnabledForPM() && (oldMember != null) && !oldMember.isEnabledForPM()) {
                    HspPMEnabledDimensionGovernor pmEnabledDimGovernor =
                        new HspPMEnabledDimensionGovernor(dim, hspJS, -1, -1, "MSG_NUM_OF_DIM_MBRS_ENBLDFORPM_THRESHOLD_EXCEEDED2", null, false, hspJS.getSystemCfg().isAppModeSimple(), hspJS.getSystemCfg().getApplicationType(), sessionId);
                    pmEnabledDimGovernor.analyze();
                }
                updateMember(member, actionSet, memberInfo, sessionId);
                actionSet.addAction(new HspMemberInfoAction(), HspActionSet.UPDATE, memberInfo);
            } else {
                // Add the governor check before adding the new member
                int potentialNumOfMbrsImported = getMembersCache(dim.getDimId()).getNumElements() + 1;
                HspDimensionGovernor dimGovernor = new HspDimensionGovernor(dim, hspJS, -1, potentialNumOfMbrsImported, sessionId);
                HspPMEnabledDimensionGovernor pmEnabledDimGovernor = null;
                if (member.isEnabledForPM())
                    pmEnabledDimGovernor = new HspPMEnabledDimensionGovernor(dim, hspJS, -1, -1, "MSG_NUM_OF_DIM_MBRS_ENBLDFORPM_THRESHOLD_EXCEEDED2", null, false, hspJS.getSystemCfg().isAppModeSimple(), hspJS.getSystemCfg().getApplicationType(), sessionId);
                HspGovernor governor = pmEnabledDimGovernor == null ? dimGovernor : new HspCompositeGovernor(new HspGovernor[] { pmEnabledDimGovernor, dimGovernor });
                governor.analyze();

                if (hspJS.getSystemCfg().isAppModeSimple() || HspHealthCheckCriteriaFactory.isCustomerHertz() || HspHealthCheckCriteriaFactory.isCustomerTDECU()) {
                    HspMembersAcrossAllDimensionsGovernor mbrsGovernor =
                        new HspMembersAcrossAllDimensionsGovernor(dim, hspJS, HspConstants.PLAN_TYPE_ALL, potentialNumOfMbrsImported, !(HspHealthCheckCriteriaFactory.isCustomerHertz() || HspHealthCheckCriteriaFactory.isCustomerTDECU()), sessionId);
                    mbrsGovernor.analyze();
                }

                HspMemberOnFlyDetail parentMofDetail = getMemberOnTheFlyDetail(member.getParentId(), sessionId);
                boolean doesParentAllowDynamicChildren = parentMofDetail != null;
                if (strategy == DynamicChildStrategy.ALWAYS_DYNAMIC && !doesParentAllowDynamicChildren) {
                    parent = getDimMember(member.getDimId(), member.getParentId());
                    if (parent == null) // check parent
                        throw new RuntimeException("Invalid Parent Id: " + member.getParentId());
                    Properties props = new Properties();
                    props.setProperty("PARENT_NAME", parent.getMemberName());
                    props.setProperty("MEMBER_NAME", member.getMemberName());
                    throw new HspRuntimeException("MSG_ADD_DYN_MBR_DYNCHILD_NOT_ENABLED", props);
                }

                if (doesParentAllowDynamicChildren) {
                    switch (strategy) {
                    case DYNAMIC_IF_AVAILABLE:
                        // If all buckets are used, cancel early otherwise
                        // try to use a bucket.  If it fails, the call will
                        // be repeated with NEVER_DYNAMIC
                        if (parentMofDetail != null && parentMofDetail.getCurrentBucket() >= parentMofDetail.getOldBucketSize())
                            break;
                    case ALWAYS_DYNAMIC:
                    case ALWAYS_DYNAMIC_IF_ENABLED:
                        // Try to iniialize the oldName with the Essbase name, if an exception is thrown,
                        // the entire saveMember() transaction will be rolled back and may be retried. If
                        // the parent for the member being saved is not enabled for dynamic children, the
                        // oldName will not be set.
                        initializeEssbaseNameForMemberOnTheFly(actionSet, member, sessionId);
                    }
                }

                if (!(member instanceof HspDPMember) && !(member instanceof HspPMMember) && !(member instanceof HspMDMember)) {
                    // Set the order for the newly added member for invalid combination rules to work. The updateable cache will fire an event which when handled
                    // will flush the VCRules processor/cache. This member order is not persisted in the member tables, it stays in the members in cache,
                    // but will get set to the order usage table. If an exception is thrown, the entire saveMember() transaction will be rolled back and
                    // may be retried.
                    initializeOrderForMember(actionSet, member, sessionId);
                }
                memberInfo = new HspMemberInfo(member, null, sessionId);
                addMember(member, actionSet, memberInfo, sessionId);
                actionSet.addAction(new HspMemberInfoAction(), HspActionSet.ADD, memberInfo);
            }

            // Only performs action if member is member from base time period hierarchy
            // updateCalendarPositions(member, actionSet, sessionId);
        } catch (Exception e) {
            actionSet.rollback();
            throw e;
        } catch (Throwable t) {
            actionSet.rollback();
            throw new Exception(t.toString());
        }
        return memberInfo;
    }

    /**
     * Checks if for the passed in member, there already exists a member with same name. Can be used for new
     * member or updating an existing member.
     * @param member
     * @param isExistingMember : Pass false for new member and true for existing member.
     * @return
     */
    private boolean hasDuplicateMemberName(HspMember member, boolean isExistingMember) {
        HspMember duplicateMember = getDimMember(member.getDimId(), member.getObjectName());
        if (duplicateMember == null) {
            return false;
        }

        return (isExistingMember) ? (member.getIdForCache() != duplicateMember.getIdForCache()) : true;
    }

    private void initializeOrderForMember(HspActionSet actionSet, HspMember member, int sessionId) throws Exception {
        HspUtils.verifyArgumentNotNull(actionSet, "actionSet");
        // Add a new Order Usage row which will fail with an integrity violation if any other server grabbed the same order first.
        // In this case the entire saveMember() call will be retried
        HspMemberOrderUsage usage = new HspMemberOrderUsage();
        usage.setDimId(member.getDimId());
        actionSet.addAction(new HspMemberOrderUsageAction(), HspActionSet.ADD, usage);

        // Now that HspUpdateableCache handles order changes a copy expression
        // action is no longer needed.
        // actionSet.addAction(new HspCopyExpressionAction("order", "val2.orderUsed"), HspActionSet.CUSTOM, new DualVal<Object, Object>(member, usage));
    }

    private int getHierarchyTypeForMember(HspCube cube, HspMember member) {
        if (cube.getType() != HspConstants.ASO_CUBE)
            return HspConstants.ESS_MULTIPLE_HIERARCHY_NOT_SET;
        int hierarchyType = HspConstants.ESS_MULTIPLE_HIERARCHY_NOT_SET;

        HspMember mbr = member;
        // Walk the member parents until we get a hierarchy type set or hit the root member
        while (mbr.getId() != mbr.getDimId()) {
            if (mbr.getHierarchyType() != HspConstants.ESS_MULTIPLE_HIERARCHY_NOT_SET)
                hierarchyType = mbr.getHierarchyType();

            mbr = getDimMemberWithoutPtProps(mbr.getDimId(), mbr.getParentId());
        }
        return hierarchyType;
    }

    public boolean isDynamicMember(HspMember member, int sessionId) {
        //        HspMember parent = getDimMember(member.getDimId(), member.getParentId());
        //        return parent.getMemberOnFlyDetail() != null; // ((HspMember)member.getParent()).getMemberOnFlyDetail() != null;
        return getMemberOnTheFlyDetail(member.getParentId(), sessionId) != null;
    }

    public boolean isMemberEnabledForDynamicChildren(int memberId, int sessionId) {
        return getMemberOnTheFlyDetail(memberId, sessionId) != null;
    }

    private void validateDTSMemberAliases(HspMember member, Object[][] aliases) {
        if (member != null && aliases != null && member.isDTSMember() && ((HspTimePeriod)(member)).getDTSGeneration() > 0) {
            for (int i = 0; i < aliases.length; i++) {
                if (aliases[i][1] != null && DTSGenToMbrNameHash.get(((String)(aliases[i][1])).toLowerCase()) != null)
                    throw new InvalidDimensionMemberNameException((String)(aliases[i][1]), InvalidDimensionMemberNameException.MSG_ERR_DTS_NAME_CONFLICT);
            }
        }
    }

    private int addMemberAliases(HspActionSet actionSet, HspMember member, Object[][] aliases, HspMemberInfo memberInfo) {
        // Return the number of changes made (actions submitted) for use by smart-save logic
        // Don't allow adding of alisaes to shared members
        int numberOfChanges = 0;
        if ((aliases != null && aliases.length > 0) && (!member.isSharedMember())) {
            validateAliases(aliases); // check the names and table id's
            CachedObjectKeyDef aliasKeyDef = AliasMemberIdTableIdKeyDef.ALIAS_MEMBER_ID_TABLE_ID_KEY;
            HspAliasAction action = new HspAliasAction();
            Set<HspAlias> specifiedAliases = new HashSet<HspAlias>();
            for (int i = 0; i < aliases.length; i++) {
                //HspAttributeMemberBinding binding = aliases[i];
                HspAlias alias = new HspAlias();
                alias.setAliastblId((Integer)aliases[i][0]);
                alias.setObjectName((String)aliases[i][1]);
                alias.setMemberId(member.getId());
                Object aliasKey = aliasKeyDef.createKey(alias);
                //TODO: Make the exception / message below real.
                if (specifiedAliases.contains(aliasKey))
                    throw new IllegalArgumentException("Can not set the same alias twice for the same member in a single save request.");

                specifiedAliases.add(alias);
                actionSet.addAction(action, HspActionSet.ADD, alias);
                numberOfChanges++;
            }

            if (memberInfo != null && numberOfChanges > 0) {
                memberInfo.setAliasInfo(aliases);
            }
        }
        return numberOfChanges;
    }

    public int updateMemberAliases(HspActionSet actionSet, HspMember member, Object[][] aliasInfo, boolean update, int sessionId) {
        return updateMemberAliases(actionSet, member, aliasInfo, update, null, sessionId);
    }

    private int updateMemberAliases(HspActionSet actionSet, HspMember member, Object[][] aliasInfo, boolean update, HspMemberInfo memberInfo, int sessionId) {
        // Return the number of changes made (actions submitted) for use by smart-save logic
        int numberOfChanges = 0;
        // If adding alias that is the same as what's on the base, ignore
        // If adding an alias to a shared that is different than base's, add it
        // how do we get shared & base member infor if member isn't in cache?
        // Don't allow adding of alisaes to shared members for now
        if ((aliasInfo != null && aliasInfo.length > 0) && (!member.isSharedMember())) {
            validateAliases(aliasInfo); // validaet the alias table id's and alias names
            validateDTSMemberAliases(member, aliasInfo);
            CachedObjectKeyDef aliasKeyDef = AliasMemberIdTableIdKeyDef.ALIAS_MEMBER_ID_TABLE_ID_KEY;
            HspAliasAction action = new HspAliasAction();
            Set specifiedAliases = new HashSet();
            Map<Object, HspAlias> existingAliasesMap = new HashMap<Object, HspAlias>();
            List<HspAlias> oldAliases = null;
            List<HspAlias> newAliases = null;

            // First, add all existing bindings into existingBindingsMap
            // Then, if a binding matches an existing binding, clone the existing
            // binding and update it, removing the existing binding form the map.
            // If the binding does not exist, then add the new binding.
            // When we are done, the map will contain all existing bindings
            // that were not updated, so call delete on each of these bindings.

            // Also, db2 was deadlocking if .doActions(false) was called before the follwing was called. So
            // now we see if we are doing an add or update. If it's an add we don't need to call the following
            // because it will have no existing aliases, if it's an update we call it. Defects 385633 & 386642
            Vector<HspAlias> existingAliases = null;
            if (update)
                existingAliases = hspAlsDB.getAliasesForMember(member, sessionId);
            if (existingAliases != null) {
                if (memberInfo != null && oldAliases == null)
                    oldAliases = new ArrayList<HspAlias>();
                int numAliases = existingAliases.size();
                for (int i = 0; i < numAliases; i++) {
                    HspAlias alias = existingAliases.get(i);
                    if (alias != null) {
                        Object aliasKey = aliasKeyDef.createKey(alias);
                        existingAliasesMap.put(aliasKey, alias);
                        if (memberInfo != null && oldAliases != null)
                            oldAliases.add((HspAlias)alias.cloneForUpdate());
                    }
                }
            }
            if (memberInfo != null && oldAliases != null) {
                memberInfo.setOldAliasInfo(getAliasInfo(oldAliases));
            }

            // Create a list of alias objects of those specified in the call param
            List<HspAlias> aliases = new ArrayList<HspAlias>(); // create an array list of alias objects while we're at it.
            for (int i = 0; i < aliasInfo.length; i++) {
                HspAlias alias = new HspAlias();
                alias.setAliastblId((Integer)aliasInfo[i][0]);
                alias.setObjectName((String)aliasInfo[i][1]);
                alias.setMemberId(member.getId());
                aliases.add(alias);
            }

            for (int i = 0; i < aliases.size(); i++) {
                HspAlias alias = aliases.get(i);
                Object aliasKey = aliasKeyDef.createKey(alias);
                //TODO: Make the exception / message below real.
                if (specifiedAliases.contains(aliasKey))
                    throw new IllegalArgumentException("Can not set the same alias twice for the same member in a single save request.");

                specifiedAliases.add(aliasKey);
                HspAlias existingAlias = existingAliasesMap.get(aliasKey);
                // if there is an exising alias for the table we're doing an update so clone the member
                // and add an update action, or if the alias value passed in is null we're going to delete the
                // existing
                if (existingAlias != null) {
                    existingAlias = (HspAlias)existingAlias.cloneForUpdate();
                    String value = alias.getName();
                    // Here we're deleting an existing alias - null was specified as alias name
                    if (value == null || value.length() == 0) {
                        actionSet.addAction(action, HspActionSet.DELETE, existingAlias);
                        numberOfChanges++;
                    }
                    // else an alias binding exists and a non-null value was specified so were doing
                    // an update - replacing one value with another
                    else {
                        // Only update the alias if it's being set to something other than what it is already
                        if (existingAlias.getObjectName().compareTo(value) != 0) {
                            existingAlias.setObjectName(value); // update the name (everything else stays as is)
                            actionSet.addAction(action, HspActionSet.UPDATE, existingAlias);
                            if (memberInfo != null && newAliases == null)
                                newAliases = new ArrayList<HspAlias>();
                            if (newAliases != null)
                                newAliases.add((HspAlias)existingAlias.cloneForUpdate());
                            numberOfChanges++;
                        }
                    }
                    existingAliasesMap.remove(aliasKey);
                }
                // else we're adding a new alias cause no binding existed before
                else {
                    String value = alias.getName();
                    // if a null alias was specified for a table that has no existing alias just igonore it
                    if (value == null || value.length() == 0)
                        continue;

                    // If the member has not been added yet, then add an action
                    // to copy the id from the member to the alias
                    if (member.getId() <= 0)
                        actionSet.addAction(new HspCopyIdAction("memberId", "objectId"), HspActionSet.CUSTOM, new DualVal(alias, member));
                    actionSet.addAction(action, HspActionSet.ADD, alias);
                    if (memberInfo != null && newAliases == null)
                        newAliases = new ArrayList<HspAlias>();
                    if (newAliases != null)
                        newAliases.add((HspAlias)alias.cloneForUpdate());
                    numberOfChanges++;
                }
            }
            // Every alias that wasn't specified for update or added must now be deleted.
            for (Iterator<HspAlias> it = existingAliasesMap.values().iterator(); it.hasNext(); ) {
                HspAlias alias = it.next();
                actionSet.addAction(action, HspActionSet.DELETE, alias);
                numberOfChanges++;
            }

            if (memberInfo != null && newAliases != null && !newAliases.isEmpty()) {
                memberInfo.setAliasInfo(getAliasInfo(newAliases));
            } else if (numberOfChanges == 0 && (newAliases == null || newAliases.isEmpty()) && oldAliases != null) {
                // Old and new aliases are the same.
                memberInfo.setAliasInfo(getAliasInfo(oldAliases));
            }
        }

        return numberOfChanges;
    }

    private Object[][] getAliasInfo(List<HspAlias> aliases) {
        Object[][] aliasInfo = new Object[aliases.size()][2];
        int i = 0;
        for (HspAlias alias : aliases) {
            aliasInfo[i][0] = alias.getAliastblId();
            aliasInfo[i][1] = alias.getObjectName();
            i++;
        }
        return aliasInfo;
    }

    public int deleteMemberAliases(HspActionSet actionSet, HspMember member, int sessionId) {
        return deleteMemberAliases(actionSet, member, null, sessionId);
    }

    private int deleteMemberAliases(HspActionSet actionSet, HspMember member, HspMemberInfo memberInfo, int sessionId) {
        // Return the number of changes made (actions submitted) for use by smart-save logic
        int numberOfChanges = 0;
        HspAliasAction action = new HspAliasAction();
        Vector<HspAlias> existingAliases = hspAlsDB.getAliasesForMember(member, sessionId);
        if (existingAliases != null) {
            int numAliases = existingAliases.size();
            for (int i = 0; i < numAliases; i++) {
                HspAlias alias = existingAliases.get(i);
                // Only delete the alias if it's explicitly assigned to this member (to avoid casefor shareds to avoid
                // firing delete twice and getting an optimistic concurrency exception).
                if (alias != null && alias.getMemberId() == member.getId()) {
                    actionSet.addAction(action, HspActionSet.DELETE, alias);
                    numberOfChanges++;
                }
            }
            if (memberInfo != null) {
                memberInfo.setOldAliasInfo(getAliasInfo(existingAliases));
                memberInfo.setAliasInfo(getAliasInfo(existingAliases));
            }
        }
        return numberOfChanges;
    }


    private int addMemberAttributeBindings(HspActionSet actionSet, HspMember member, HspAttributeMemberBinding[] bindings, HspMemberInfo memberInfo) {
        // return number of changes made (actions submitted) for use with smart-save logic.
        int numberOfChanges = 0;
        if (bindings != null && bindings.length > 0) {
            CachedObjectKeyDef attribKeyDef = AttributeBindingAttribDimIdMemberIdPerspectiveKeyDef.ATTRIBUTE_BINDING_ATTRIB_DIM_ID_MEMBER_ID_PERSPECTIVE_KEY_DEF;
            HspAttributeMemberBindingAction action = new HspAttributeMemberBindingAction();
            Set<Object> specifiedBindings = new HashSet<Object>();

            for (int i = 0; i < bindings.length; i++) {
                HspAttributeMemberBinding binding = bindings[i];
                if (binding != null) {
                    binding.setMemberId(member.getId());
                    Object attribKey = attribKeyDef.createKey(binding);
                    //TODO: Make the exception / message below real.
                    if (specifiedBindings.contains(attribKey))
                        throw new IllegalArgumentException("Can not set the same attribute twice for the same member in a single save request.");

                    specifiedBindings.add(attribKey);
                    // If the member has not been added yet, then add an action
                    // to copy the id from the member to the binding
                    if (member.getId() <= 0)
                        actionSet.addAction(new HspCopyIdAction("memberId", "objectId"), HspActionSet.CUSTOM, new DualVal(binding, member));
                    actionSet.addAction(action, HspActionSet.ADD, binding);
                    numberOfChanges++;
                }
            }

            if (memberInfo != null && numberOfChanges > 0) {
                List<HspAttributeMemberBinding> newBndgs = new ArrayList<HspAttributeMemberBinding>();
                for (HspAttributeMemberBinding binding : bindings)
                    newBndgs.add((HspAttributeMemberBinding)binding.cloneForUpdate());

                memberInfo.setAttribBnds(newBndgs.toArray(new HspAttributeMemberBinding[newBndgs.size()]));
            }
        }
        return numberOfChanges;
    }

    private int updateMemberAttributeBindings(HspActionSet actionSet, int memberId, int dimId, HspAttributeMemberBinding[] bindings, HspMemberInfo memberInfo) {
        // Return the number of changes made (actions submitted) and return them for use
        // with smart-save logic.
        int numberOfChanges = 0;
        List<HspAttributeDimension> attribDims = getAttributeDimensionsForBaseDim(dimId);
        if (attribDims != null && attribDims.size() > 0) {
            CachedObjectKeyDef attribKeyDef = AttributeBindingAttribDimIdMemberIdPerspectiveKeyDef.ATTRIBUTE_BINDING_ATTRIB_DIM_ID_MEMBER_ID_PERSPECTIVE_KEY_DEF;
            HspAttributeMemberBindingAction action = new HspAttributeMemberBindingAction();
            Set<Object> specifiedBindings = new HashSet<Object>();
            Map<Object, HspAttributeMemberBinding> existingBindingsMap = new HashMap<Object, HspAttributeMemberBinding>();
            List<HspAttributeMemberBinding> oldBngs = null;
            List<HspAttributeMemberBinding> newBndgs = null;
            // First, add all existing bindings into existingBindingsMap
            // Then, if a binding matches an existing binding, clone the existing
            // binding and update it, removing the existing binding form the map.
            // If the binding does not exist, then add the new binding.
            // When we are done, the map will contain all existing bindings
            // that were not updated, so call delete on each of these bindings.
            for (Iterator<HspAttributeDimension> it = attribDims.iterator(); it.hasNext(); ) {
                HspAttributeDimension attribDim = it.next();
                Vector<HspAttributeMemberBinding> existingBindings = hspFMDB.getAttributeMemberBindingsForAllPerspectives(attribDim.getId(), memberId);
                if (existingBindings != null) {
                    if (memberInfo != null && oldBngs == null)
                        oldBngs = new ArrayList<HspAttributeMemberBinding>();
                    for (Iterator<HspAttributeMemberBinding> it2 = existingBindings.iterator(); it2.hasNext(); ) {
                        HspAttributeMemberBinding binding = it2.next();
                        Object attribKey = attribKeyDef.createKey(binding);
                        existingBindingsMap.put(attribKey, binding);
                        if (memberInfo != null && oldBngs != null)
                            oldBngs.add((HspAttributeMemberBinding)binding.cloneForUpdate());
                    }
                }
            }
            if (memberInfo != null && oldBngs != null)
                memberInfo.setOldAttribBnds(oldBngs.toArray(new HspAttributeMemberBinding[oldBngs.size()]));

            if (bindings != null) {
                for (int i = 0; i < bindings.length; i++) {
                    HspAttributeMemberBinding binding = bindings[i];
                    if (binding != null) {
                        binding.setMemberId(memberId);
                        Object attribKey = attribKeyDef.createKey(binding);
                        //TODO: Make the exception / message below real.
                        if (specifiedBindings.contains(attribKey))
                            throw new IllegalArgumentException("Can not set the same attribute twice for the same member in a single save request.");

                        specifiedBindings.add(attribKey);
                        HspAttributeMemberBinding existingBinding = existingBindingsMap.get(attribKey);
                        if (existingBinding != null) {
                            // The old cube refresh examined time stamps. For the new cube refresh we no longer need to do updates
                            // so the changes get picked up - an outline change is sufficient.
                            // Reimplement smartsave. The previous fix was incorrect. This is the case where a different attribute FROM THE SAME
                            // attriubute dimension binding is being saved. If the attribute binding (attribId) is different generate an update, if the
                            // attribute is the same don't (smartSave). Prior to this the code was always updating the binding
                            // whether it changed or not.
                            if (existingBinding.getAttributeId() != binding.getAttributeId()) {
                                existingBinding = (HspAttributeMemberBinding)existingBinding.cloneForUpdate();
                                existingBinding.setAttributeId(binding.getAttributeId());
                                actionSet.addAction(action, HspActionSet.UPDATE, existingBinding);
                                if (memberInfo != null && newBndgs == null)
                                    newBndgs = new ArrayList<HspAttributeMemberBinding>();
                                if (newBndgs != null)
                                    newBndgs.add((HspAttributeMemberBinding)existingBinding.cloneForUpdate());
                                numberOfChanges++;
                            }
                            existingBindingsMap.remove(attribKey);
                        } else {
                            actionSet.addAction(action, HspActionSet.ADD, binding);
                            if (memberInfo != null && newBndgs == null)
                                newBndgs = new ArrayList<HspAttributeMemberBinding>();
                            if (newBndgs != null)
                                newBndgs.add((HspAttributeMemberBinding)binding.cloneForUpdate());
                            numberOfChanges++;
                        }
                    }
                }
                // moved to inside bindings != null check so that bindings are
                // not removed when no bindings are passed into this method
                for (Iterator<HspAttributeMemberBinding> it = existingBindingsMap.values().iterator(); it.hasNext(); ) {
                    HspAttributeMemberBinding binding = it.next();
                    actionSet.addAction(action, HspActionSet.DELETE, binding);
                    numberOfChanges++;
                }
            }

            // Due to a pre existing very old bug, this function gets called twice and second time it comes in with no bindings. In this case do not set the new bindings again.
            if (numberOfChanges == 0 && bindings == null && memberInfo != null && memberInfo.getMember() != null && !HspUtils.isNullOrEmpty(memberInfo.getMember().getAttributesToBeSaved()))
                return numberOfChanges;

            if (memberInfo != null && newBndgs != null && !newBndgs.isEmpty())
                memberInfo.setAttribBnds(newBndgs.toArray(new HspAttributeMemberBinding[newBndgs.size()]));
            else if (numberOfChanges == 0 && (newBndgs == null || newBndgs.isEmpty()) && oldBngs != null) {
                // Old and new attributes are the same.
                memberInfo.setAttribBnds(oldBngs.toArray(new HspAttributeMemberBinding[oldBngs.size()]));
            }
        }
        return numberOfChanges;
    }

    private int deleteMemberAttributeBindings(HspActionSet actionSet, int memberId, int dimId, HspMemberInfo memberInfo) {
        // Return the number of changes made (actions submitted) for use by smart-save logic.
        int numberOfChanges = 0;
        HspAttributeMemberBindingAction action = new HspAttributeMemberBindingAction();
        List<HspAttributeDimension> attribDims = getAttributeDimensionsForBaseDim(dimId);
        if (attribDims != null && attribDims.size() > 0) {
            for (Iterator<HspAttributeDimension> it = attribDims.iterator(); it.hasNext(); ) {
                HspAttributeDimension attribDim = it.next();
                Vector<HspAttributeMemberBinding> existingBindings = hspFMDB.getAttributeMemberBindingsForAllPerspectives(attribDim.getId(), memberId);
                if (existingBindings != null) {
                    List<HspAttributeMemberBinding> newBndgs = null;
                    List<HspAttributeMemberBinding> oldBngs = null;
                    if (memberInfo != null) {
                        oldBngs = new ArrayList<HspAttributeMemberBinding>();
                        newBndgs = new ArrayList<HspAttributeMemberBinding>();
                    }
                    for (Iterator<HspAttributeMemberBinding> it2 = existingBindings.iterator(); it2.hasNext(); ) {
                        HspAttributeMemberBinding binding = it2.next();
                        actionSet.addAction(action, HspActionSet.DELETE, binding);
                        numberOfChanges++;
                        if (memberInfo != null) {
                            oldBngs.add((HspAttributeMemberBinding)binding.cloneForUpdate());
                            newBndgs.add((HspAttributeMemberBinding)binding.cloneForUpdate());
                        }
                    }
                    if (memberInfo != null) {
                        memberInfo.setOldAttribBnds(oldBngs.toArray(new HspAttributeMemberBinding[oldBngs.size()]));
                        memberInfo.setAttribBnds(newBndgs.toArray(new HspAttributeMemberBinding[newBndgs.size()]));
                    }
                }
            }
        }
        return numberOfChanges;
    }

    private boolean memberHasAttributesAssigned(HspMember member) {
        if (member == null || member.getId() < 0)
            return false;
        List<HspAttributeDimension> attribDims = getAttributeDimensionsForBaseDim(member.getDimId());
        if (attribDims != null && attribDims.size() > 0) {
            for (Iterator<HspAttributeDimension> it = attribDims.iterator(); it.hasNext(); ) {
                HspAttributeDimension attribDim = it.next();
                Vector<HspAttributeMemberBinding> existingBindings = hspFMDB.getAttributeMemberBindingsForAllPerspectives(attribDim.getId(), member.getId());
                if (existingBindings != null && existingBindings.size() > 0)
                    return true;
            }
        }
        return false;
    }

    /*
        // if aliases need to be changed add them to the action set
        if (member.getShouldSaveAlias())
        {
            HspMemberAliasesAction aliasesAction = new HspMemberAliasesAction();
            actionSet.addAction(aliasesAction, actionSet.UPDATE, member);
        }
        // if attributes need to be changed add them to the action set
        if (member.getShouldSaveAttributes())
        {
            HspMemberAttributesAction attributesAction = new HspMemberAttributesAction();
            actionSet.addAction(attributesAction, actionSet.UPDATE, member);
        }
        // if justUpdateTimestamp was specified (true) AND aliases or attributes changed add a timestamp action. But
        // DON'T add a timestamp action if the attributes or aliases DIDN'T change.
        if ((member.getShouldSaveAlias()) || (member.getShouldSaveAttributes()))
        {
            HspTimestampAction timestampAction = new HspTimestampAction();
            actionSet.addAction(timestampAction, actionSet.UPDATE, member);
            //System.out.println("timestamp updated for member "+member.getName());
        }
        */

    private int addMemberUDABindings(HspActionSet actionSet, HspMember member, HspUDABinding[] bindings, HspMemberInfo memberInfo) {
        // return number of changes made (actions submitted) for use with smart-save logic.
        int numberOfChanges = 0;
        if (bindings != null && bindings.length > 0) {
            CachedObjectKeyDef bindingKeyDef = UDABindingMemberIdUDAIdKeyDef.UDA_BINDING_MEMBER_ID_UDA_ID_KEY_DEF;
            HspUDABindingAction action = new HspUDABindingAction();
            Set specifiedBindings = new HashSet();

            for (int i = 0; i < bindings.length; i++) {
                HspUDABinding binding = bindings[i];
                if (binding != null) {
                    binding.setMemberId(member.getId());
                    Object bindingKey = bindingKeyDef.createKey(binding);
                    //TODO: Make the exception / message below real.
                    if (specifiedBindings.contains(bindingKey))
                        throw new IllegalArgumentException("Can not set the same uda twice for the same member in a single save request.");

                    specifiedBindings.add(bindingKey);
                    // If the member has not been added yet, then add an action
                    // to copy the id from the member to the binding
                    if (member.getId() <= 0)
                        actionSet.addAction(new HspCopyIdAction("memberId", "objectId"), HspActionSet.CUSTOM, new DualVal(binding, member));
                    actionSet.addAction(action, HspActionSet.ADD, binding);
                    numberOfChanges++;
                }
            }

            if (memberInfo != null && numberOfChanges > 0) {
                List<HspUDABinding> newBndgs = new ArrayList<HspUDABinding>();
                for (HspUDABinding binding : bindings)
                    newBndgs.add((HspUDABinding)binding.cloneForUpdate());

                memberInfo.setUdaInfo(newBndgs.toArray(new HspUDABinding[newBndgs.size()]));
            }
        }
        return numberOfChanges;
    }

    public int updateMemberUDABindings(HspActionSet actionSet, int memberId, int dimId, HspUDABinding[] bindings) {
        return updateMemberUDABindings(actionSet, memberId, dimId, bindings, null);
    }

    private int updateMemberUDABindings(HspActionSet actionSet, int memberId, int dimId, HspUDABinding[] bindings, HspMemberInfo memberInfo) {
        // Return the number of changes made (actions submitted) and return them for use
        // with smart-save logic.
        int numberOfChanges = 0;
        if (bindings != null) {
            CachedObjectKeyDef bindingKeyDef = UDABindingMemberIdUDAIdKeyDef.UDA_BINDING_MEMBER_ID_UDA_ID_KEY_DEF;
            HspUDABindingAction action = new HspUDABindingAction();
            Set specifiedBindings = new HashSet();
            Map<Object, HspUDABinding> existingBindingsMap = new HashMap<Object, HspUDABinding>();
            List<HspUDABinding> newBndgs = null;
            List<HspUDABinding> oldBngs = null;
            // First, add all existing bindings into existingBindingsMap
            // Then, if a binding matches an existing binding, clone the existing
            // binding and update it, removing the existing binding form the map.
            // If the binding does not exist, then add the new binding.
            // When we are done, the map will contain all existing bindings
            // that were not updated, so call delete on each of these bindings.
            Vector<HspUDABinding> existingBindings = getUDABindings(memberId);
            if (existingBindings != null) {
                if (memberInfo != null)
                    oldBngs = new ArrayList<HspUDABinding>();

                for (Iterator<HspUDABinding> it2 = existingBindings.iterator(); it2.hasNext(); ) {
                    HspUDABinding binding = it2.next();
                    Object bindingKey = bindingKeyDef.createKey(binding);
                    existingBindingsMap.put(bindingKey, binding);
                    if (memberInfo != null)
                        oldBngs.add((HspUDABinding)binding.cloneForUpdate());
                }
                if (memberInfo != null)
                    memberInfo.setOldUdaInfo(oldBngs.toArray(new HspUDABinding[oldBngs.size()]));
            }

            for (int i = 0; i < bindings.length; i++) {
                HspUDABinding binding = bindings[i];
                if (binding != null) {
                    binding.setMemberId(memberId);
                    Object bindingKey = bindingKeyDef.createKey(binding);
                    //TODO: Make the exception / message below real.
                    if (specifiedBindings.contains(bindingKey))
                        throw new IllegalArgumentException("Can not set the same uda twice for the same member in a single save request.");


                    specifiedBindings.add(bindingKey);
                    HspUDABinding existingBinding = existingBindingsMap.get(bindingKey);
                    if (existingBinding != null) {
                        // No change is needed if the UDA already exists.
                        existingBindingsMap.remove(bindingKey);
                        if (memberInfo != null && newBndgs == null)
                            newBndgs = new ArrayList<HspUDABinding>();
                        if (newBndgs != null)
                            newBndgs.add((HspUDABinding)binding.cloneForUpdate());
                    } else {
                        actionSet.addAction(action, HspActionSet.ADD, binding);
                        numberOfChanges++;
                        if (memberInfo != null && newBndgs == null)
                            newBndgs = new ArrayList<HspUDABinding>();
                        if (newBndgs != null)
                            newBndgs.add((HspUDABinding)binding.cloneForUpdate());
                    }
                }
            }
            if (newBndgs != null)
                memberInfo.setUdaInfo(newBndgs.toArray(new HspUDABinding[newBndgs.size()]));

            for (Iterator<HspUDABinding> it = existingBindingsMap.values().iterator(); it.hasNext(); ) {
                HspUDABinding binding = it.next();
                actionSet.addAction(action, HspActionSet.DELETE, binding);
                numberOfChanges++;
            }
        }
        return numberOfChanges;
    }

    private int deleteMemberUDABindings(HspActionSet actionSet, int memberId, int dimId, HspMemberInfo memberInfo) {
        // Return the number of changes made (actions submitted) for use by smart-save logic.
        int numberOfChanges = 0;
        HspUDABindingAction action = new HspUDABindingAction();
        Vector<HspUDABinding> existingBindings = getUDABindings(memberId);
        if (existingBindings != null) {
            List<HspUDABinding> newBndgs = null;
            List<HspUDABinding> oldBngs = null;
            if (memberInfo != null) {
                newBndgs = new ArrayList<HspUDABinding>();
                oldBngs = new ArrayList<HspUDABinding>();
            }
            for (Iterator<HspUDABinding> it2 = existingBindings.iterator(); it2.hasNext(); ) {
                HspUDABinding binding = it2.next();
                actionSet.addAction(action, HspActionSet.DELETE, binding);
                numberOfChanges++;
                if (memberInfo != null) {
                    newBndgs.add((HspUDABinding)binding.cloneForUpdate());
                    oldBngs.add((HspUDABinding)binding.cloneForUpdate());
                }
            }
            if (memberInfo != null) {
                memberInfo.setOldUdaInfo(oldBngs.toArray(new HspUDABinding[oldBngs.size()]));
                memberInfo.setUdaInfo(newBndgs.toArray(new HspUDABinding[newBndgs.size()]));
            }
        }
        return numberOfChanges;
    }

    private synchronized void updateMember(HspMember member, HspActionSet actionSet, HspMemberInfo memberInfo, int sessionId) throws Exception {
        if (member == null)
            throw new IllegalArgumentException("Cannot update a null member");


        HspAttributeMemberBinding[] attributeBindings = member.getAttributesToBeSaved();
        member.setAttributesToBeSaved(null);
        Object[][] aliases = member.getAliasesToBeSaved();
        member.setAliasesToBeSaved(null);
        HspUDABinding[] udaBindings = member.getUDAsToBeSaved();
        member.setUDAsToBeSaved(null);
        //	    HspMemberOnFlyDetail mbrOnFlyDetail = member.getMemberOnFlyDetailToBeSaved();
        //	    member.setMemberOnFlyDetailToBeSaved(null);

        // make sure object type checks uses this as opposed to getObjectType - necessitated for shared members
        HspMember baseMember = getBaseMemberOrFailIfExpected(member, actionSet);
        int objectType = member.isSharedMember() ? baseMember.getObjectType() : member.getObjectType();

        HspMember oldMember = getDimMember(member.getDimId(), member.getId()); // get cached 'old' member
        int dimId = member.getDimId();
        if (oldMember == null)
            throw new IllegalArgumentException("Unknown member id " + member.getId() + " for dimension " + dimId);

        HspMember parent = getDimMember(member.getDimId(), member.getParentId(), actionSet);
        if (parent == null) // check parent
            throw new RuntimeException("Invalid Parent Id: " + member.getParentId());

        // restore original PTProps if member.PTProps is null since
        // it may not have been filled in.  member.PTProps should be
        // empty and not null to clear PTProps
        if (member.getMemberPTPropsToBeSaved() == null && oldMember.getMemberPTPropsToBeSaved() != null)
            member.setMemberPTPropsToBeSaved(oldMember.getMemberPTPropsToBeSaved());

        //HspMember oldParent = getDimMember(oldMember.getDimId(), oldMember.getParentId());
        HspMember oldParent = getDimMember(oldMember.getDimId(), oldMember.getParentId(), actionSet);
        if (oldParent == null) // check parent
            throw new RuntimeException("Invalid Old Parent Id: " + member.getParentId());


        boolean move = oldMember.getParentId() != member.getParentId();

        //If we're moving a subtree, we have to perform some checks so we don't end up with an invalid hierarchy.
        if (move) {
            // Don't allow shared members to be moved to a new parent, but amongst siblings is ok.
            //			if (member.isSharedMember())
            //				throw new RuntimeException("Shared members cannot be moved to a new parent.");


            // Check to make sure move of a base member won't make it a sibling of one of it's shareds
            validateBaseWontBeSiblingOfShared(member, actionSet, sessionId);

            // position has already been set.  member.setPosition(getLastChildPosition(parent)+ kMemberPositionIncrement);

            // Generate a hashtable of all shared members in the subtree being moved, where the key of the
            // hash is baseMemberId
            Hashtable sharedMemberHashTable = generateSharedMemberHashtableForSubtree(member);

            // Check that the destination for the move of a subtree is not a child within that subtree.
            // To perform the check take the destination (new parent id) and walk up the dimension tree to the root.
            // Throw an exception if the source (member to move) is hit in this walk.

            // Issue a seperate exception if new parent specified for move is the member itself
            if (member.getParentId() == member.getId())
                throw new HspRuntimeException("MSG_INVALID_MOVE_NEW_PARENT_IS_MEMBER");


            // Also check to see that move would not result in Base Member being an ancestor of a corresponding
            // Shared Member.
            HspMember ancestor = parent;
            while (ancestor.getId() != ancestor.getDimId()) {
                if (ancestor.getId() == member.getId())
                    throw new HspRuntimeException("MSG_INVALID_MOVE_CHILD_IS_DESTINATION");


                Integer key = ancestor.getId();
                if (sharedMemberHashTable.get(key) != null)
                    throw new HspRuntimeException("MSG_INVALID_MOVE_SHARED_BASE_AS_ANCESTOR");

                int tempAncestorId = ancestor.getParentId();
                ancestor = getDimMember(ancestor.getDimId(), tempAncestorId, actionSet);
                if (ancestor == null)
                    throw new InvalidMemberException(Integer.toString(tempAncestorId));


            }

        }

        member.setHasChildren(oldMember.hasChildren());
        addOrRemSbVerParent(member, oldMember, sessionId, 0, actionSet);
        // Add the member to be updated
        HspAction action = createAction(objectType);
        // Check how many memberPTProps we have so we'll know how many change events to fire on update
        Vector<HspMemberPTProps> mbrProps = getMemberPTProps(member.getId(), sessionId);
        if (mbrProps != null && mbrProps.size() > 0) {
            action.setParameter("mbrProps", mbrProps);
        }
        // Only add the member to the action set if it is not already there
        if (actionSet.getCachedObject(member) != member) {
            actionSet.addAction(action, HspActionSet.UPDATE, member);
            updateMemberAttributeBindings(actionSet, member.getId(), member.getDimId(), attributeBindings, memberInfo);
            updateMemberAliases(actionSet, member, aliases, true, memberInfo, sessionId);
            updateMemberUDABindings(actionSet, member.getId(), member.getDimId(), udaBindings, memberInfo);
            //            updateMemberOnTheFlyDetail(actionSet, member.getId(), mbrOnFlyDetail, sessionId);
        }
        // If we're moving we may need to update the new and old parent's hasChildren flags appropriately
        if (move) {
            // If the new parent's hasChildren flag is false, set it to true now cause we're moving under it
            if (!parent.hasChildren()) {
                HspMember tempParent = (HspMember)actionSet.getCachedObject(parent);
                if (tempParent == null)
                    parent = (HspMember)parent.cloneForUpdate();
                else
                    parent = tempParent;
                parent.setHasChildren(true);
                actionSet.addAction(action, HspActionSet.UPDATE, parent);
            }
            // If the old parent had a child and it's the member we're moving, set its hasChildren flag to false
            // cause we moved out from under it and we were it's only child.
            if ((oldParent.getNumChildren() == 1) && (((HspMember)(oldParent.getChildren().firstElement())).getId() == member.getId())) {
                HspMember tempParent = (HspMember)actionSet.getCachedObject(oldParent);
                if (tempParent == null)
                    oldParent = (HspMember)oldParent.cloneForUpdate();
                else
                    oldParent = tempParent;
                oldParent.setHasChildren(false);
                actionSet.addAction(action, HspActionSet.UPDATE, oldParent);
            }
        }

        // We need to update shared members if: 1) the base member's name changes, or 2) the base's plan
        // types change, or 3) (entity) base's default currency changes, 4) account 445 setting changes
        boolean someEntityPropertiesChanged = false;
        if (objectType == HspConstants.kDimensionEntity) {
            if ((((HspEntity)member).getDefaultCurrency() != ((HspEntity)oldMember).getDefaultCurrency()) || (((HspEntity)member).getEmployeeId() != ((HspEntity)oldMember).getEmployeeId()) ||
                (((HspEntity)member).getRequisitionNumber() != ((HspEntity)oldMember).getRequisitionNumber()) || (((HspEntity)member).getEntityType() != ((HspEntity)oldMember).getEntityType()))
                someEntityPropertiesChanged = true;
        }

        boolean someAccountPropertiesChanged = false;
        if (objectType == HspConstants.kDimensionAccount) {
            if ((((HspAccount)member).getUse445() != ((HspAccount)oldMember).getUse445()) || (((HspAccount)member).getTimeBalance() != ((HspAccount)oldMember).getTimeBalance()) || (((HspAccount)member).getSkipValue() != ((HspAccount)oldMember).getSkipValue()) ||
                (((HspAccount)member).getAccountType() != ((HspAccount)oldMember).getAccountType()) || (((HspAccount)member).getSubAccountType() != ((HspAccount)oldMember).getSubAccountType()) ||
                (((HspAccount)member).getVarianceRep() != ((HspAccount)oldMember).getVarianceRep()) || (((HspAccount)member).getCurrencyRate() != ((HspAccount)oldMember).getCurrencyRate()) || (member.getDataType() != oldMember.getDataType()) ||
                (((HspAccount)member).getSrcPlanType() != ((HspAccount)oldMember).getSrcPlanType()) || (((HspAccount)member).getTimeBalance() != ((HspAccount)oldMember).getTimeBalance()))
                someAccountPropertiesChanged = true;
        }

        if (!member.isSharedMember())
            if ((member.getObjectName().compareTo(oldMember.getName()) != 0) || (member.getUsedIn() != oldMember.getUsedIn()) || (member.getEnumerationId() != oldMember.getEnumerationId()) || (someEntityPropertiesChanged) ||
                (!HspUtils.equals(member.getDescription(), oldMember.getDescription())) || (member.getShouldSaveAlias()) || (someAccountPropertiesChanged))
                updateSharedMembers(member, actionSet, sessionId);

        updateChildMembers(member, actionSet, action, move, sessionId);

        //If UPDATE_PUS_ON_HAL_LOAD != false, then process planning units
        String updatePUs = hspJS.getAppProperty(HspJSHome.UPDATE_PUS_ON_HAL_LOAD);
        if (!"false".equalsIgnoreCase(updatePUs)) {
            // update process management
            //            List<HspPlanningUnit> pus = hspPMDB.getPlanningUnitsForMember(member, sessionId);
            //            HspPUAction puAction = new HspPUAction();
            //            for (HspPlanningUnit pu : pus)
            //            {
            //                HspPlanningUnit tempPU = (HspPlanningUnit)actionSet.getCachedObject(pu);
            //                if (tempPU != null)
            //                    pu = tempPU;
            //                else
            //                    pu = (HspPlanningUnit)pu.cloneForUpdate();
            //
            //                //This tree only needs to be updated if the planning unit was started
            //                if ((pu != null) && (pu.getProcessState() != HspConstants.PU_NOT_STARTED))
            //                {	//update the original parent
            //                    hspPMDB.updateParent(pu, HspConstants.PM_ACTION_ACTION_ID_START, puAction, actionSet, true, sessionId);
            //                    //update the new parent;
            //                    pu.setParentId(member.getParentId());
            //                    hspPMDB.updateParent(pu, HspConstants.PM_ACTION_ACTION_ID_START, puAction, actionSet, sessionId);
            //                }
            //            }


            // update process managememt if moving an entity
            if ((move) && (dimId == HspConstants.kDimensionEntity)) {
                HspEntity entity = (HspEntity)member;
                Vector<HspScenario> scenarios = hspPMDB.getPMScenarios(false, sessionId);
                Vector<HspVersion> versions = hspPMDB.getPMVersions(false, sessionId);

                if ((scenarios != null) && (versions != null)) {
                    HspPUAction puAction = new HspPUAction();
                    for (int loop1 = 0; loop1 < scenarios.size(); loop1++) {
                        for (int loop2 = 0; loop2 < versions.size(); loop2++) {
                            HspScenario scenario = scenarios.elementAt(loop1);
                            HspVersion version = versions.elementAt(loop2);
                            if ((scenario != null) && (version != null)) {
                                HspPlanningUnit pu = hspPMDB.getPlanningUnit(scenario.getId(), version.getId(), entity.getId());
                                //This tree only needs to be updated if the planning unit was started
                                if ((pu != null) && (pu.getProcessState() != HspConstants.PU_NOT_STARTED)) { //update the original parent
                                    hspPMDB.updateParent(pu, HspConstants.PM_ACTION_ACTION_ID_START, puAction, actionSet, true, sessionId);
                                    //update the new parent;
                                    pu = (HspPlanningUnit)pu.cloneForUpdate();
                                    pu.setParentId(entity.getParentId());
                                    hspPMDB.updateParent(pu, HspConstants.PM_ACTION_ACTION_ID_START, puAction, actionSet, sessionId);
                                }
                            }
                        }
                    }
                }
            }
        }
        //        HashSet<Integer> pmDimIdsOfGeneratedPMActions = createAncillaryPMActions(member, (move ? oldParent.getId() : 0), HspActionSet.UPDATE, actionSet, sessionId);
        //        if (pmDimIdsOfGeneratedPMActions != null)
        //            for (Integer pmDimId : pmDimIdsOfGeneratedPMActions)
        //                hspPMDB.savePMDimDef(hspPMDB.getPMDimDef(pmDimId, sessionId), actionSet, sessionId);
        //actionSet.doActions();
        //Cache will be automatically updated/invalidated by the HspAction class
    }

    private void addOrRemSbVerParent(HspMember member, HspMember oldMember, int sessionId, int opType, HspActionSet actionSet) throws Exception {
        //optype indicates the type of operation
        //1 - Add member
        //2 - Remove member
        //3 - Indicates rename of member
        //Ignore the member sandbox enabled check only for update
        if (member.getDimId() == HspConstants.kDimensionVersion && (((HspVersion)member).isSandboxEnabled() || opType == 0)) {

            //In case of update, old member will not be null
            //If sandboxenabled checkbox has been unchecked as part of update, consider as deletion of the sandbox parent member
            //If sandboxenabled checkbox has been checked as part of update, consider as addition of the sandbox parent member
            //If member has been renamed, rename the sandbox parent member - i.e., delete the existing member and add the new member
            if (oldMember != null && (((HspVersion)member).isSandboxEnabled() != ((HspVersion)oldMember).isSandboxEnabled() || (!member.getMemberName().equals(oldMember.getMemberName())))) {
                if (((HspVersion)member).isSandboxEnabled()) {
                    opType = 1;
                    if (!member.getMemberName().equals(oldMember.getMemberName())) {
                        opType = 3;
                    }
                } else {
                    opType = 2;
                }
            }

            if (opType == 1 || opType == 3) {
                //Add member
                //If already exists ignore
                HspVersion sbVersionParent = getVersion(((HspVersion)member).getSandboxParentName());
                if (sbVersionParent == null) {
                    sbVersionParent = getSbVerParent(((HspVersion)member).getSandboxParentName(), sessionId);
                    saveMember(sbVersionParent, sessionId);
                } else {
                    //The removable property would have not got seeded
                    sbVersionParent = (HspVersion)sbVersionParent.cloneForUpdate();
                    sbVersionParent.setRemovable(getMemberByName(HspConstants.HSP_DIM_VIEW).getRemovable());
                    sbVersionParent.setMemberOnFlyDetailToBeSaved(getMemberOnTheFlyDetail(sbVersionParent.getId(), sessionId));
                    sbVersionParent.setOverrideLock(true);
                    saveMember(sbVersionParent, sessionId);
                }
            }
            if (opType == 2 || opType == 3) {
                //In case of optype 3 - Indicates rename,Remove the old member's sb parent
                //Remove member
                HspVersion sbVersionParent = null;
                if (opType == 3) {
                    sbVersionParent = getVersion(((HspVersion)oldMember).getSandboxParentName());
                } else {
                    sbVersionParent = getVersion(((HspVersion)member).getSandboxParentName());
                }
                //Remove the sandbox history related to the sandboxes_<version>
                if (sbVersionParent != null) {
                    if (sbVersionParent.isObjectLocked()) {
                        sbVersionParent.setOverrideLock(true);
                    }

                    //Delete children of Sandboxes_<Version>
                    deleteMembers(sbVersionParent, false, false, sessionId);

                    HspAction action = createAction(sbVersionParent.getObjectType());
                    actionSet.addAction(action, HspActionSet.DELETE, sbVersionParent);
                }
            }
        }
    }

    public synchronized void updateSharedMembers(HspMember member, HspActionSet actionSet, int sessionId) throws Exception {
        // This method should be called when: 1) a base member's name changes, 2) plan types change on
        // account or entity base member, 3) currency changes on an entity base member, or 4) description
        // change on base member.
        // accounts and entities can be shared, plan types for both, currency for entities
        // need to fire account, entity actions so caches get updated correctly, use generic credate action method
        // For now, this method is called when a base member's name changes. When this happens the
        // shared members names need to be updated as well.
        // don't check for null arg's here
        if (member.isSharedMember())
            throw new RuntimeException("Expected base member, shared member was specified.");


        Vector sharedMembers = this.getSharedMembersOfBase(member.getDimId(), member.getId(), actionSet, sessionId);
        if (sharedMembers == null)
            return;

        //Object[][] newBaseAliases = member.getAliasesToBeSaved();
        //TODO: for each shared, clone the base to pick up new state and reset id's, alias, and other shared prop's on the clone to match origianl shared
        HspAction action = this.createAction(member.getObjectType());
        for (int i = 0; i < sharedMembers.size(); i++) {
            // If its an entity pick up any default currency and plan type changes
            if (member.getDimId() == HspConstants.kDimensionEntity) {
                HspEntity shared = (HspEntity)sharedMembers.elementAt(i);
                if (shared == null)
                    continue;
                HspEntity tempShared = (HspEntity)actionSet.getCachedObject(shared);
                if (tempShared == null)
                    shared = (HspEntity)shared.cloneForUpdate();
                else
                    shared = tempShared;
                shared.setDefaultCurrency(((HspEntity)member).getDefaultCurrency());
                int parentPlanTypes = ((this.getDimMember(shared.getDimId(), shared.getParentId(), actionSet))).getUsedIn();
                shared.setUsedIn(parentPlanTypes & member.getUsedIn() & shared.getUsedIn()); // mask off plan types not on base
                shared.setObjectName(member.getObjectName());
                shared.setDescription(member.getDescription());
                shared.setEmployeeId(((HspEntity)member).getEmployeeId());
                shared.setRequisitionNumber(((HspEntity)member).getRequisitionNumber());
                shared.setEntityType(((HspEntity)member).getEntityType());
                shared.setEnabledForPM(member.isEnabledForPM());
                shared.setEnumerationId(member.getEnumerationId());

                setUniqueName(shared, actionSet);

                actionSet.addAction(action, HspActionSet.UPDATE, shared);
            }
            // else if its an account pick up plan type changes
            else if (member.getDimId() == HspConstants.kDimensionAccount) {
                HspAccount shared = (HspAccount)sharedMembers.elementAt(i);
                if (shared == null)
                    continue;
                HspAccount tempShared = (HspAccount)actionSet.getCachedObject(shared);
                if (tempShared == null)
                    shared = (HspAccount)shared.cloneForUpdate();
                else
                    shared = tempShared;
                int parentPlanTypes = (this.getDimMember(shared.getDimId(), shared.getParentId(), actionSet)).getUsedIn();
                shared.setUsedIn(parentPlanTypes & member.getUsedIn() & shared.getUsedIn()); // mask off plan types not on base
                shared.setObjectName(member.getObjectName());
                HspAccount baseMember = (HspAccount)member;
                shared.setDataType(baseMember.getDataType());
                shared.setAccountType(baseMember.getAccountType());
                shared.setSubAccountType(baseMember.getSubAccountType());
                shared.setCurrencyRate(baseMember.getCurrencyRate());
                shared.setPlanningMethod(baseMember.getPlanningMethod());
                shared.setSkipValue(baseMember.getSkipValue());
                shared.setTimeBalance(baseMember.getTimeBalance());
                shared.setUse445(baseMember.getUse445());
                shared.setVarianceRep(baseMember.getVarianceRep());
                shared.setDescription(member.getDescription());
                shared.setEnumerationId(baseMember.getEnumerationId());

                setUniqueName(shared, actionSet);

                actionSet.addAction(action, HspActionSet.UPDATE, shared);
            }
            // else just cast as a member and pick up a name change.
            else // custom members and currencies...
            {
                HspMember shared = (HspMember)sharedMembers.elementAt(i);
                if (shared == null)
                    continue;
                HspMember tempShared = (HspMember)actionSet.getCachedObject(shared);
                if (tempShared == null)
                    shared = (HspMember)shared.cloneForUpdate();
                else
                    shared = tempShared;
                shared.setObjectName(member.getObjectName());
                int dimensionPlanTypes = ((this.getDimRoot(shared.getDimId()))).getUsedIn();
                int parentPlanTypes = (this.getDimMember(shared.getDimId(), shared.getParentId(), actionSet)).getUsedIn();
                //TODO: 3.4 validate dimensionplantypes & is not needed
                // is this necessary? won't the caches be flushed when the root is modified
                if (member.getDimId() > HspConstants.kDimensionLast)
                    shared.setUsedIn(parentPlanTypes & member.getUsedIn() & shared.getUsedIn()); // mask off plan types not on base
                else
                    shared.setUsedIn(dimensionPlanTypes & member.getUsedIn()); // mask off plan types not on base
                shared.setDescription(member.getDescription());
                shared.setEnumerationId(member.getEnumerationId());

                setUniqueName(shared, actionSet);

                actionSet.addAction(action, HspActionSet.UPDATE, shared);
            }

        }
    }

    // This method is package friendly so that HspCALDB can call it.

    synchronized void updateChildMembers(HspMember member, HspActionSet actionSet, HspAction action, boolean move, int sessionId) throws Exception {
        // don't check for null arg's here
        // make sure object type checks uses this as opposed to getObjectType - necessitated for shared members
        HspMember baseMember = getBaseMemberOrFailIfExpected(member, actionSet);
        int objectType = member.isSharedMember() ? baseMember.getObjectType() : member.getObjectType();

        Vector<HspMember> children = getChildMembers(member.getDimId(), member.getId(), true, actionSet, sessionId);
        if (children != null) {
            if (member.isSharedMember())
                HspLogger.trace("ALERT: Found a shared member with children!");
            else {
                for (HspMember child : children) {
                    if (child == null)
                        continue;

                    boolean updated = false;
                    // Object level sets here
                    // generation check/set
                    if (move) {
                        int childGeneration = child.getGeneration();
                        int correctGeneration = member.getGeneration() + 1;
                        if (childGeneration != correctGeneration) {
                            if (child.isReadOnly())
                                child = (HspMember)child.cloneForUpdate();
                            updated = true;
                            child.setGeneration(correctGeneration);
                        }
                    }

                    // Member-level sets
                    // Verify UsedIn
                    int usedIn = getUsedIn(member);
                    int childUsedIn = getUsedIn(child);
                    int correctUsedIn = (usedIn & childUsedIn);
                    if (childUsedIn != correctUsedIn) {
                        if (child.isReadOnly())
                            child = (HspMember)child.cloneForUpdate();
                        updated = true;
                        if (child.isObjectLocked())
                            child.setOverrideLock(true);
                        setUsedIn(child, correctUsedIn);
                        if (!child.isSharedMember())
                            updateSharedMembers(child, actionSet, sessionId); // update any shareds of this child
                    }

                    // Update uniqueName if needed
                    String correctUniqueName = getUniqueName(child, actionSet);
                    if (!HspUtils.equals(child.getUniqueName(), correctUniqueName)) {
                        if (child.isReadOnly())
                            child = (HspMember)child.cloneForUpdate();
                        updated = true;
                        child.setUniqueName(correctUniqueName);
                    }

                    // Account specific checks
                    if (objectType == HspConstants.gObjType_Account) {
                        HspAccount account = (HspAccount)child;
                        int sourcePlanType = account.getSrcPlanType();
                        if ((sourcePlanType & correctUsedIn) != sourcePlanType) {
                            if (child.isReadOnly())
                                child = (HspMember)child.cloneForUpdate();
                            updated = true;
                            account = (HspAccount)child;
                            setSourcePlanType(account); // set the source plan type (and if it isn't valid, make it so)
                        }
                    }

                    //After all checks, if the child was updated, save the child and process his children
                    if (updated) {
                        boolean isObjectSeeded = hspJS.getFeatureDB(sessionId).isObjectSeeded(child.getId(), child.getObjectType());
                        boolean shouldTrack = HspModuleInfo.getModuleUser() == null || (!HspModuleInfo.getModuleUser().equals(HspModuleInfo.MODULE_USER));
                        List<HspDiff> diffList = null;
                        if (isObjectSeeded && shouldTrack) {
                            ReadOnlyCachedObject objInCache = hspJS.getFeatureDB(sessionId).getPlanningObject(child.getObjectName(), child.getObjectType(), sessionId, child.getParentId());
                            diffList = child.getDiffList(hspJS, objInCache, sessionId);

                            //Below condition is satisfied for Shared Members for which UsedIn has been changed i.e. diffList will NOT be EMPTY
                            if (child.isSharedMember() && diffList != null && !diffList.isEmpty()) {
                                HspDiffUtil.addParameterToDiffList("PARENT_NAME", child.getParent().getName(), diffList);
                            }
                        }

                        if (actionSet.getCachedObject(child) == null)
                            actionSet.addAction(action, HspActionSet.UPDATE, child);
                        updateChildMembers(child, actionSet, action, move, sessionId);

                        if (isObjectSeeded && shouldTrack && diffList != null && diffList.size() > 0) {
                            int artifactId = hspJS.getFeatureDB(sessionId).getModuleArtifactDetailByObjId(child.getId(), child.getObjectType()).getId();
                            hspJS.getFeatureDB(sessionId).addAuditRecord(HspActionSet.UPDATE, diffList, artifactId, sessionId);
                        }
                    }
                }
            }
        }
    }

    private synchronized void addMember(HspMember member, HspActionSet actionSet, HspMemberInfo memberInfo, int sessionId) throws Exception {
        HspAttributeMemberBinding[] attributeBindings = member.getAttributesToBeSaved();
        member.setAttributesToBeSaved(null);
        Object[][] aliases = member.getAliasesToBeSaved();
        member.setAliasesToBeSaved(null);
        HspUDABinding[] udaBindings = member.getUDAsToBeSaved();
        member.setUDAsToBeSaved(null);
        HspMemberOnFlyDetail mbrOnFlyDetail = member.getMemberOnFlyDetailToBeSaved();
        member.setMemberOnFlyDetailToBeSaved(null);

        HspMember parent = getDimMember(member.getDimId(), member.getParentId());
        if (parent == null) // check parent
            throw new RuntimeException("Invalid Parent Id: " + member.getParentId());

        addOrRemSbVerParent(member, null, sessionId, 1, null);
        HspDimension dim = getDimRoot(member.getDimId());
        HspAction action = createAction(dim.getObjectType());
        action.setParameter("SecuredDimension", dim.getEnforceSecurity());

        // Throw an exception if a shared member add is attempted with a parentId equal to its Base Member's
        // parentId, i.e. the base and shared will be siblings. This is illegal for updates as well, but ANY
        // move of a shared member is illegal so let that exception be thrown for updates.
        if (member.isSharedMember()) {
            HspMember baseMember = getBaseMemberOrFailIfExpected(member, actionSet);
            if (member.getParentId() == baseMember.getParentId())
                throw new HspRuntimeException("MSG_INVALID_ADD_SHARED_AS_SIBLING_OF_BASE", new RuntimeException("A Shared Member cannot be added as a sibling of its Base Member."));

            // Check to make sure this shared member's parent is not an ascendent (on path to dim. root)
            HspMember ancestor = parent;
            while (ancestor.getId() != ancestor.getDimId()) {
                if (ancestor.getId() == baseMember.getId())
                    throw new HspRuntimeException("MSG_INVALID_ADD_SHARED_BASE_IS_ASCENDENT", new RuntimeException("A Shared Member cannot be added where its corresponding Base Member would be an ascendent."));

                int tempAncestorId = ancestor.getParentId();
                ancestor = getDimMember(ancestor.getDimId(), tempAncestorId);
                if (ancestor == null)
                    throw new InvalidMemberException(Integer.toString(tempAncestorId));
            }
            validateSharedWillBeUniqueSharedUnderParent(member, actionSet, sessionId);
        }
        //member.setPosition(getLastChildPosition(parent) + kMemberPositionIncrement); // set the position
        //member.setPosition(HspConstants.kMemberPositionLastSibling);
        member.setHasChildren(false);
        actionSet.addAction(action, HspActionSet.ADD, member);

        // If a shared member is added and the base member's unique name is
        // null then set the base member's unique name and add to actionSet.
        // This must be done after the shared member is added to the actionSet.
        if (member.isSharedMember()) {
            HspMember baseMember = getBaseMemberOrFailIfExpected(member, actionSet);
            if (baseMember.getUniqueName() == null) {
                if (baseMember.isReadOnly()) {
                    baseMember = (HspMember)baseMember.cloneForUpdate();
                    // Add to actionSet if not already present
                    actionSet.addAction(action, HspActionSet.UPDATE, baseMember);
                }
                setUniqueName(baseMember, actionSet);
            }
        }

        // Do actions to get the member's memberId;
        // If the parent never had children, set its hasChildren flag to true cause we're adding it's 1st new child
        // This is done automatically in HspObjectAction
        //        if (!parent.hasChildren()) {
        //            parent = (HspMember)parent.cloneForUpdate();
        //            parent.setHasChildren(true);
        //            actionSet.addAction(action, HspActionSet.UPDATE, parent);
        //        }
        //If the member is currency, check if predefined
        //if predefined, override the original record with override currency type
        if (member.getObjectType() == HspConstants.gObjType_Currency || member.getObjectType() == HspConstants.gObjType_SimpleCurrency) {
            HspCurrency currency = (HspCurrency)member;
            HspCurrency preDefinedCurrencyUpd = hspCurDB.getPredefinedCurrency(currency.getCurrencyCode());
            if (preDefinedCurrencyUpd != null) {
                HspCurrency preDefinedCurrency = (HspCurrency)preDefinedCurrencyUpd.cloneForUpdate();
                preDefinedCurrency.setCurrencyType(HspConstants.gintCurTypeOverriddenPreDefined);
                actionSet.addAction(action, HspActionSet.UPDATE, preDefinedCurrency);
            }
        }
        //TODO: Validate it is OK to remove doActions(false)
        //actionSet.doActions(false);
        addMemberAttributeBindings(actionSet, member, attributeBindings, memberInfo);
        updateMemberAliases(actionSet, member, aliases, false, memberInfo, sessionId);
        addMemberUDABindings(actionSet, member, udaBindings, memberInfo);
        if (mbrOnFlyDetail != null)
            updateMemberOnTheFlyDetail(actionSet, member, /*.getId(), member.getDimId()*/mbrOnFlyDetail, memberInfo, sessionId);

        //        actionSet.doActions(false);
        //        addMemberAsPMSecondary(member, actionSet, sessionId);

    }

    private HspVersion getSbVerParent(String versionParentName, int sessionId) {
        HspVersion sbVersionParent = new HspVersion();
        HspVersion parent = getVersion(HspConstants.MEMBER_NAME_SANDBOXES_VERSION);
        HspMember hspView = getMemberByName(HspConstants.HSP_DIM_VIEW);
        sbVersionParent.setObjectName(versionParentName);
        sbVersionParent.setVersionType(HspConstants.VERSION_OFFICIAL_BU);
        sbVersionParent.setOwnerId(hspStateMgr.getUserId(sessionId));
        sbVersionParent.setParentId(parent.getId());
        sbVersionParent.setEnabledForPM(parent.isEnabledForPM());
        sbVersionParent.setAccessType(HspConstants.VERSION_ACCESS_PUBLIC);
        sbVersionParent.setConsolOp(hspView.getUsedIn(), HspConstants.kDataConsolIgnore);
        sbVersionParent.setHasChildren(false);
        sbVersionParent.setUsedIn(hspView.getUsedIn());
        sbVersionParent.setDimId(parent.getDimId());
        sbVersionParent.setDataStorage(parent.getDataStorage());
        sbVersionParent.setTwopassCalc(parent.getTwopassCalc());
        sbVersionParent.setDataType(parent.getDataType());
        sbVersionParent.setObjectType(HspConstants.gObjType_Version);
        sbVersionParent.setRemovable(2);
        HspMemberOnFlyDetail memOnTheFlyDetail = new HspMemberOnFlyDetail();
        memOnTheFlyDetail.setBucketSize(HspConstants.NUMBER_OF_SANDBOXES);
        memOnTheFlyDetail.setCreatorAccessMode(HspConstants.ACCESS_READ_WRITE);
        sbVersionParent.setMemberOnFlyDetailToBeSaved(memOnTheFlyDetail);
        return sbVersionParent;
    }

    public void addAttributeMember(HspMember attributeMember, int sessionId) throws Exception {
        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        addAttributeMember(attributeMember, actionSet, sessionId);
        actionSet.optimizeForBatch(false, new DualVal(HspMemberOnFlyCompositeAction.class, ActionMethod.ADD), new DualVal(HspAttributeMember.class, ActionMethod.ADD), new DualVal(HspAttributeMember.class, ActionMethod.UPDATE));
        actionSet.doActions();
    }

    // This is being left private for the time being becase allowing it to be called and having it's actions bundled with
    // an external actionset has not been debugged.

    private synchronized void addAttributeMember(HspMember attributeMember, HspActionSet actionSet, int sessionId) throws Exception {
        try {
            hspStateMgr.verify(sessionId);

            if (attributeMember == null)
                throw new IllegalArgumentException("attributeMember argument may not be null");


            //validateFlatDimensionMembersParent(attributeMember);

            HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
            // is this hspattribure member? if so, cloneforupdate and use set relevant fields
            HspMember parent = getDimMember(attributeMember.getDimId(), attributeMember.getParentId());
            if (parent == null)
                throw new RuntimeException("Invalid parent id for \"" + attributeMember.getName() + "\" attribute.");
            if (!((parent.getObjectType() == HspConstants.gObjType_AttributeMember) || (parent.getParentId() == HspConstants.gFolder_AttrDims)))
                throw new RuntimeException("Invalid object type for parent attribute: " + parent.getName());

            // prevent addind kids to an attribure that is assigned to a member(s)
            Vector<HspAttributeMemberBinding> bindings = hspFMDB.getAttributeBindingsForAttribute(parent.getDimId(), parent.getId());
            if ((bindings != null) && (bindings.size() > 0)) {
                Properties p = new Properties();
                p.put("MEMBER_NAME", parent.getName());
                throw new HspRuntimeException("MSG_NO_KIDS_ALLOWED_ON_BOUND_ATTRIBUTE", p);
            }

            HspAttributeMember member = (HspAttributeMember)attributeMember; //new HspAttributeMember();

            String attributeName = attributeMember.getName();
            if (attributeName != null)
                attributeName = attributeName.trim();
            HspAttributeDimension attDim = getAttributeDimension(attributeMember.getDimId());
            if (attDim == null)
                throw new RuntimeException("Invaliid attribute dimension id specified on attribute member \"" + attributeName + "\"");

            validateAttributeName(attributeName); // this checks length to 80 (column width) but we pre-pend characters below, so need to recheck length.

            // if we have a referenced attribute dim then make sure we can find referenced member.
            if (attDim.getReferenceDimId() > 0) {
                HspMember refMbr = getDimMember(attDim.getReferenceDimId(), attributeName);
                if (refMbr == null) {
                    Properties p = new Properties();
                    p.put("MEMBER_NAME", attributeName);
                    throw new HspRuntimeException("MSG_CANNOT_FIND_REFERENCED_MBR_ATTR", p);
                }
                member.setReferenceMemberId(refMbr.getId());
            }


            // if we're loading a hire date make sure its somewhat reasonable
            if (parent.getPsMemberId() == HspPlanningSpecificMemberIdConstants.WF_ATTR_HIRE_DATE_ID)
                member.setObjectName(validateDateString(attributeName));
            else {
                if (attDim != null && attDim.getAttributeType() == HspConstants.kDataAttributeDate) {
                    try {
                        String dateString = validateDateString(attributeName);
                        member.setObjectName(dateString);
                    } catch (HspRuntimeException x) {
                        member.setObjectName(attributeName);
                        // Ignore HspRuntimeException - since we support string members
                        // at non-level zero for attribute members (date,number)
                    }
                } else
                    member.setObjectName(attributeName);
                // all but text attribute types should have the <dimId>_ prefix prepended in the unique names table
                member.setDimId(attDim.getId());
                setAttributeUniqueNamePrefixId(member);
            }
            //member.setObjectId(-1);
            //member.setPosition(getLastChildPosition(parent) + kMemberPositionIncrement); // set the position
            //member.setPosition(HspConstants.kMemberPositionLastSibling);
            member.setDimId(parent.getDimId());
            member.setParentId(parent.getId());
            member.setChildren(null);
            member.setParent(parent);
            member.setObjectType(HspConstants.gObjType_AttributeMember);
            member.setHasChildren(false);
            member.setGeneration(parent.getGeneration() + 1);
            member.setUsedIn(parent.getUsedIn());
            //member.setPsMemberId(attributeMember.getPsMemberId());
            member.setEnabledForPM(false);
            //member.setRemovable(attributeMember.isRemovable());
            //member.setPosition(attributeMember.getPosition());
            //Frist, we validate the add
            //Next, we create the ActionSet and action, and then populate the action set
            HspDimension dim = getDimRoot(attributeMember.getDimId());
            actionSet.addVerifyTimestampAction(dim);

            HspAttributeMemberAction action = new HspAttributeMemberAction();

            setMemberAndSiblingPositions(member, actionSet, true, sessionId);
            // pick up changes to member (possibly) made in position setting
            HspAttributeMember tempMember = (HspAttributeMember)actionSet.getCachedObject(member);
            if (tempMember != null) {
                member = tempMember;
            }

            actionSet.addAction(action, HspActionSet.ADD, member);

            // update the parent's hasChildren flag if necessary
            if (!parent.hasChildren()) {
                parent = (HspAttributeMember)parent.cloneForUpdate();
                parent.setHasChildren(true);
                actionSet.addAction(action, HspActionSet.UPDATE, parent);
            }

            //Comment ACB move this actionSet.doActions(false) to after updateMemberAliases call.
            actionSet.doActions(false);


            //HspAttributeMemberBinding[] attributeBindings = member.getAttributesToBeSaved();
            member.setAttributesToBeSaved(null);
            Object[][] aliases = attributeMember.getAliasesToBeSaved();
            member.setAliasesToBeSaved(null);
            HspUDABinding[] udaBindings = attributeMember.getUDAsToBeSaved();
            member.setUDAsToBeSaved(null);

            // add attribute, member, and alias bindings
            //addMemberAttributeBindings(actionSet, member.getId(), member.getDimId(), attributeBindings);
            updateMemberAliases(actionSet, member, aliases, false, sessionId);
            addMemberUDABindings(actionSet, member, udaBindings, null);

            //  actionSet.doActions(false);
            // actions are commited externally
            //Cache will be automatically updated/invalidated by the HspAction class
        } catch (Exception e) {
            actionSet.rollback();
            throw e;
        } catch (Throwable t) {
            actionSet.rollback();
            throw new Exception(t.toString());
        }

    }

    private void setAttributeUniqueNamePrefixId(HspAttributeMember attributeMember) {
        // all but text attribute types should have the <dimId>_ prefix prepended in the unique names table
        // when set to 0 no prefix is prepended for entry in unique names table.
        // since the prefix id is not persisted this has to be reset after some cache fetch/updates so unique name table
        // entry retains prefix.
        HspUtils.verifyArgumentNotNull(attributeMember, "attributeMember");
        HspAttributeDimension attributeDimension = getAttributeDimension(attributeMember.getDimId());
        if (attributeDimension == null)
            throw new RuntimeException("Unable to fetch attribute dimension");


        // all but text attribute types should have the <dimId>_ prefix prepended in the unique names table
        // when set to 0 no prefix is prepended for entry in unique names table.
        if (attributeDimension != null && attributeDimension.getAttributeType() != HspConstants.kDataAttributeText)
            attributeMember.setUniqueIdPrefixInt(validateAttributeMemberName(attributeMember));
        else
            attributeMember.setUniqueIdPrefixInt(0); // 0 means don't prefix entry in unique names table
    }

    private int getBaseDimId(HspMember attributeMember) {
        HspUtils.verifyArgumentNotNull(attributeMember, "attribute member");
        HspAttributeDimension attDim = getAttributeDimension(attributeMember.dimId);
        if (attDim != null)
            return attDim.baseDimId;
        else
            return 0;
    }

    /** return base dim id **/
    private int validateAttributeMemberName(HspMember attributeMember) {
        HspUtils.verifyArgumentNotNull(attributeMember, "attribute member");
        HspAttributeDimension attDim = getAttributeDimension(attributeMember.dimId);
        String attrName = attributeMember.getName();
        if (attrName == null)
            throw new RuntimeException("Null attribute name.");

        attrName = attrName.trim(); // Bug 26534128
        int attrType = attDim.getAttributeType();
        switch (attrType) {
        case HspConstants.kDataAttributeBoolean:
            if (!HspConstants.ATTRIBUTE_TYPE_TRUE.equals(attrName) && !HspConstants.ATTRIBUTE_TYPE_FALSE.equals(attrName))
                throw new HspRuntimeException("Invalid name for boolean attribute dimension member");
            break;
        }
        if (attrType != HspConstants.kDataAttributeText) {
            // Irrelevant code removed
            if (attDim.getBaseDimId() <= 0)
                throw new HspRuntimeException("Invalid base dimension for attribute member ");
        }
        return attDim.getBaseDimId();
    }


    public void modifyAttributeMember(HspMember attributeMember, int sessionId) throws Exception {
        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        modifyAttributeMember(attributeMember, actionSet, sessionId);
        actionSet.optimizeForBatch(false, new DualVal(HspMemberOnFlyCompositeAction.class, ActionMethod.ADD), new DualVal(HspAttributeMember.class, ActionMethod.ADD), new DualVal(HspAttributeMember.class, ActionMethod.UPDATE));
        actionSet.doActions();
    }

    // This is being left private for the time being becase allowing it to be called and having it's actions bundled with
    // an external actionset has not been debugged.

    private synchronized void modifyAttributeMember(HspMember attributeMember, HspActionSet actionSet, int sessionId) throws Exception {
        try {
            HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
            // currently all we can change is the name, or text value, of the attribute
            if (attributeMember == null)
                throw new RuntimeException("Member argument null.");


            //validateFlatDimensionMembersParent(member);
            HspAttributeMember member = (HspAttributeMember)this.getAttributeMember((getAttributeDimension(attributeMember.getDimId())).getBaseDimId(), attributeMember.getId());
            if (member == null) // check parent
                throw new RuntimeException("Invalid attribute, Id: " + attributeMember.getId());


            member = (HspAttributeMember)member.cloneForUpdate();

            Object[][] aliases = attributeMember.getAliasesToBeSaved();
            member.setAliasesToBeSaved(null);

            int aliasUpdates = updateMemberAliases(actionSet, member, aliases, true, sessionId);

            if (attributeMember.propertiesEquivalentTo(member))
                return;
            // if we're loading a hire date make sure its somewhat reasonable
            HspMember parent = getDimMember(attributeMember.getDimId(), attributeMember.getParentId());
            if (parent == null)
                throw new RuntimeException("Invalid parent id for \"" + member.getName() + "\" attribute.");
            if (!((parent.getObjectType() == HspConstants.gObjType_AttributeMember) || (parent.getParentId() == HspConstants.gFolder_AttrDims)))
                throw new RuntimeException("Invalid object type for parent attribute: " + parent.getName());
            member.setParentId(attributeMember.getParentId()); // Bug 26566273
            Vector<HspAttributeMemberBinding> bindings = hspFMDB.getAttributeBindingsForAttribute(parent.getDimId(), parent.getId());
            if ((bindings != null) && (bindings.size() > 0)) {
                Properties p = new Properties();
                p.put("MEMBER_NAME", parent.getName());
                throw new HspRuntimeException("MSG_NO_KIDS_ALLOWED_ON_BOUND_ATTRIBUTE", p);
            }

            HspAttributeDimension attDim = getAttributeDimension(attributeMember.getDimId());
            if (attDim == null)
                throw new RuntimeException("Invaliid attribute dimension id specified on attribute member \"" + attributeMember.getName() + "\"");

            validateAttributeName(attributeMember.getName());

            // if we have a referenced attribute dim then make sure we can find referenced member.
            if (attDim.getReferenceDimId() > 0) {
                HspMember refMbr = getDimMember(attDim.getReferenceDimId(), attributeMember.getName());
                if (refMbr == null) {
                    Properties p = new Properties();
                    p.put("MEMBER_NAME", attributeMember.getName());
                    throw new HspRuntimeException("MSG_CANNOT_FIND_REFERENCED_MBR_ATTR", p);
                }
                member.setReferenceMemberId(refMbr.getId());
            }


            if (parent.getPsMemberId() == HspPlanningSpecificMemberIdConstants.WF_ATTR_HIRE_DATE_ID)
                member.setObjectName(validateDateString(attributeMember.getName()));
            else {
                member.setObjectName(attributeMember.getName());
                int baseDimId = validateAttributeMemberName(attributeMember);
                if (attDim != null) {
                    // Support non-uniqueness for boolean attribute members.
                    if (attDim.getAttributeType() == HspConstants.kDataAttributeBoolean)
                        member.setUniqueIdPrefixInt(baseDimId);
                    else if (attDim.getAttributeType() == HspConstants.kDataAttributeDate) {
                        try {
                            String dateString = validateDateString(attributeMember.getName());
                            member.setObjectName(dateString);
                        } catch (HspRuntimeException x) {
                            // Ignore HspRuntimeException - since we support string members
                            // at non-level zero for attribute members (date,number)
                        }
                    }
                }
            }
            // all but text attribute types should have the <dimId>_ prefix prepended in the unique names table
            setAttributeUniqueNamePrefixId(member);

            if (member.getDataStorage() == HspConstants.kDataStorageSharedMember)
                throw new RuntimeException("Shared members are not allowed in attribute dimensions.");


            member.setMarkedForDelete(attributeMember.isMarkedForDelete());
            member.setPsMemberId(attributeMember.getPsMemberId());
            member.setUuid(attributeMember.getUuid());
            // pick up position from input member in case we need to reorder member
            member.setPosition(attributeMember.getPosition());
            //         HspDimension dim = getDimRoot(attributeMember.getDimId());
            //         actionSet.addVerifyTimestampAction(dim);

            //           setMemberAndSiblingPositions(member, actionSet, true, sessionId);
            // pick up changes to member (possibly) made in position setting
            //          HspAttributeMember tempMember = (HspAttributeMember)actionSet.getCachedObject(member);
            //          if (tempMember != null)
            //              member = tempMember;

            //HspAttributeMemberBinding[] attributeBindings = member.getAttributesToBeSaved();
            member.setAttributesToBeSaved(null);
            //   Object[][] aliases = attributeMember.getAliasesToBeSaved();
            //   member.setAliasesToBeSaved(null);
            HspUDABinding[] udaBindings = attributeMember.getUDAsToBeSaved();
            member.setUDAsToBeSaved(null);

            //int attributeUpdates = updateMemberAttributeBindings(actionSet, member.getId(), member.getDimId(), attributeBindings);
            //  int aliasUpdates = updateMemberAliases(actionSet, member, aliases, true, sessionId);
            int udaUpdates = updateMemberUDABindings(actionSet, member.getId(), member.getDimId(), udaBindings, null);


            HspAttributeMemberAction action = new HspAttributeMemberAction();
            actionSet.addAction(action, HspActionSet.UPDATE, member);
            setMemberAndSiblingPositions(member, actionSet, true, sessionId);

            //   HspAttributeMemberAction action = new HspAttributeMemberAction();
            //    actionSet.addAction(action, HspActionSet.UPDATE, member);

            //            actionSet.doActions(false);
            //            // add attribute, member, and alias bindings
            //            //addMemberAttributeBindings(actionSet, member.getId(), member.getDimId(), attributeBindings);
            //            updateMemberAliases(actionSet, member, aliases, true, sessionId);
            //            addMemberUDABindings(actionSet, member.getId(), member.getDimId(), udaBindings);


        } catch (Exception e) {
            actionSet.rollback();
            throw e;
        } catch (Throwable t) {
            actionSet.rollback();
            throw new Exception(t.toString());
        }

    }

    public synchronized void deleteMembers(HspMember member, boolean shouldDeleteSubtreeRoot, boolean shouldDeletePUs, int sessionId) throws Exception {
        if (member == null) // check
            throw new RuntimeException("Invalid member: null");

        deleteMembers(member.getDimId(), member.getId(), shouldDeleteSubtreeRoot, shouldDeletePUs, sessionId);

        //addOrRemSbVerParent(member,null,sessionId,2);
    }

    public synchronized void deleteMembers(int dimensionId, String memberName, boolean shouldDeleteSubtreeRoot, boolean shouldDeletePUs, int sessionId) throws Exception {
        if ((memberName == null) || (memberName.length() <= 0))
            throw new RuntimeException("Invalid Member Name: empty.");


        HspMember member = this.getDimMember(dimensionId, memberName);
        if (member == null) // check
            throw new RuntimeException("Member not found. DimId: " + dimensionId + " Member Name: " + memberName);


        deleteMembers(member.getDimId(), member.getId(), shouldDeleteSubtreeRoot, shouldDeletePUs, sessionId);
    }

    public synchronized void deleteMembers(int dimensionId, int id, boolean shouldDeleteSubtreeRoot, boolean shouldDeletePUs, int sessionId) throws Exception {
        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        HspMember member = this.getDimMember(dimensionId, id);

        //If seeded member & the user is not module user, track the changes
        boolean isObjectSeeded = hspJS.getFeatureDB(sessionId).isObjectSeeded(member.getId(), member.getObjectType());
        boolean shouldTrack = HspModuleInfo.getModuleUser() == null || (!HspModuleInfo.getModuleUser().equals(HspModuleInfo.MODULE_USER));
        List<HspDiff> diffList = null;
        if (isObjectSeeded && shouldTrack) {
            diffList = new ArrayList<HspDiff>();
            HspDiffUtil.addParameterToDiffList("PARENT_NAME", member.getParent().getName(), diffList);
            HspDiffUtil.addParameterToDiffList("OBJECT_NAME", member.getObjectName(), diffList);
        }

        deleteMembers(dimensionId, id, shouldDeleteSubtreeRoot, shouldDeletePUs, actionSet, sessionId);

        // Optimize the action set actions for batch.  Move the delete actions
        // for the members to the end of the batch so that dependent objects
        // are deleted before the members.
        int objectType = HspConstants.gObjType_UserDefinedMember;
        if (member != null) {
            objectType = member.getObjectType();
            if (member.isSharedMember()) {
                HspMember baseMember = getBaseMember(member, actionSet);
                if (baseMember != null)
                    objectType = baseMember.getObjectType();
            }
            HspAction action = createAction(objectType);
            actionSet.optimizeForBatch(true, new DualVal(action.getClass(), ActionMethod.DELETE), new DualVal(HspAttributeDimensionAction.class, ActionMethod.DELETE), new DualVal(HspMemberInfoAction.class, ActionMethod.DELETE));
        }
        addOrRemSbVerParent(member, null, sessionId, 2, actionSet);
        if (hspJS.getSystemCfg().isSimpleMultiCurrency()) {
            deleteReportingCurrency(dimensionId, id, actionSet, sessionId);
        }
        actionSet.doActions();

        if (isObjectSeeded && shouldTrack && diffList != null && diffList.size() > 0) {
            int artifactId = hspJS.getFeatureDB(sessionId).getModuleArtifactDetailByObjId(member.getId(), member.getObjectType()).getId();
            // TODO: Verify the next line handles the rollback of the actionSet as this method maybe retried multiple times
            hspJS.getFeatureDB(sessionId).addAuditRecord(HspActionSet.DELETE, diffList, artifactId, sessionId);
        }
    }

    public synchronized void deleteMembers(int dimensionId, int id, boolean shouldDeleteParent, boolean shouldDeletePUs, HspActionSet actionSet, int sessionId) throws Exception {
        // NOTE: see note in deleteMember method regarding actionSet and deletes of shared members.
        HspMember member = getDimMember(dimensionId, id);
        if (member == null) // check
            throw new RuntimeException("Unable to retrieve member with member id " + id + " and dimension id " + dimensionId);


        Vector children = member.getChildren();
        if (children != null) {
            for (int loop1 = 0; loop1 < children.size(); loop1++) {
                //boolean updated = false;
                HspMember child = (HspMember)children.elementAt(loop1);
                if (child == null)
                    continue;
                deleteMembers(dimensionId, child.getId(), false, shouldDeletePUs, actionSet, sessionId);
                //System.out.println("deleting...: "+child.getName()+" Id: "+child.getId());
                deleteMember(dimensionId, child.getId(), shouldDeletePUs, actionSet, sessionId);
            }
        }

        // this should only be true for the very first entry and only if root of the subtree should be deleted
        // all intermediate parents will be deleted as children of the parents
        if (shouldDeleteParent) {
            //System.out.println("deleting root...: "+member.getName()+" Id: "+member.getId());
            deleteMember(dimensionId, id, shouldDeletePUs, actionSet, sessionId);
        }
    }

    /**
     * <code>deleteMembersAndRemoveDependencies</code> deletes the specified member and all of its children
     * along with all the dependencies.
     *
     * @param member    Member to be deleted
     * @param sessionId    Session ID
     * @throws Exception
     */
    public void deleteMembersAndRemoveDependencies(HspMember member, int sessionId) throws Exception {
        int appOwnerSessionId = -1;
        try {
            HspUser appOwner = hspSecDB.getApplicationOwner();
            if (appOwner == null || appOwner.getUserRole() != HspConstants.USER_BUDGET_ADMIN)
                throw new NotEnoughAccessException();


            appOwnerSessionId = hspStateMgr.createImpersonationSession(appOwner.getId(), sessionId);
            HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
            HspActionSet actionSet = new HspActionSet(hspSQL, user);
            deleteMembersAndRemoveDependencies(member, actionSet, appOwnerSessionId, sessionId);
            // perform the delete on the member
            actionSet.doActions();
        } finally {
            // Log off the impersonated session.
            if (appOwnerSessionId > 0)
                hspJS.logoff(appOwnerSessionId);
        }
    }

    private void deleteMembersAndRemoveDependencies(HspMember member, HspActionSet actionSet, int appOwnerSessionId, int sessionId) throws Exception {
        Vector children = member.getChildren();
        if (children != null) {
            // Delete children recursively
            for (int loop1 = 0; loop1 < children.size(); loop1++) {
                HspMember child = (HspMember)children.elementAt(loop1);
                if (child == null)
                    continue;

                deleteMembersAndRemoveDependencies(child, actionSet, appOwnerSessionId, sessionId);
            }
        }
        // Delete the member itself
        deleteMemberAndRemoveDependencies(member, actionSet, appOwnerSessionId, sessionId);
    }

    private synchronized void deleteMemberAndRemoveDependencies(HspMember member, HspActionSet actionSet, int appOwnerSessionId, int sessionId) throws Exception {
        if (member == null)
            throw new IllegalArgumentException("Input member for deletion cannot be null");


        HspAction frmAction = new HspUpdateFormMemberCustomAction();
        actionSet.addAction(frmAction, HspActionSet.CUSTOM, member);

        if (member.getDimId() == member.getId() && !member.isDPMember()) {
            Properties p = new Properties();
            p.put("DIM_NAME", member.getMemberName());
            throw new HspRuntimeException("MSG_DIMENSION_CANT_BE_DELETED", p);


        }
        //Only "All Years" Year member can be deleted but rest (FYs or "No Year") cannot.
        if (member.getDimId() == HspConstants.kDimensionYear && !(hspCalDB.hasAllYearsParent() && hspCalDB.getAllYearsParent().getId() == member.getId())) {
            Properties p = new Properties();
            p.put("MEMBER_NAME", member.getMemberName());
            throw new HspRuntimeException("MSG_YEAR_CANT_BE_DELETED", p);


        } else if (member.getDimId() == HspConstants.kDimensionTimePeriod) {
            HspTimePeriod t = (HspTimePeriod)member;
            if (t.getType() == HspConstants.LEAF_TP_TYPE) {
                Properties p = new Properties();
                p.put("MEMBER_NAME", member.getMemberName());
                throw new HspRuntimeException("MSG_BASEPERIOD_CANT_BE_DELETED", p);


            }
        } else if (member.getDimId() == HspConstants.kDimensionCurrency) {
            // verify that we are not deleting Local or Default currency
            if (member.getId() == HspConstants.gObject_LocalCrncyMbr)
                throw new HspRuntimeException("MSG_LOCAL_CURRENCY_CANT_BE_DELETED");

            if (member.getId() == HspConstants.gObject_kDefCrncyId)
                throw new HspRuntimeException("MSG_DEFAULT_CURRENCY_CANT_BE_DELETED");


            // Add action to clean up currency dependencies
            HspAction curAction = new HspUpdateCurrencyMemberCustomAction(HspConstants.gObject_kDefCrncyId);
            actionSet.addAction(curAction, HspActionSet.CUSTOM, member);
        } else if (member.getDimId() == HspConstants.kDimensionScenario) {
            HspAction delPUAction = new HspDeletePUsByScenarioCustomAction();
            actionSet.addAction(delPUAction, HspActionSet.CUSTOM, member);
        } else if (member.getDimId() == HspConstants.kDimensionVersion) {
            HspAction delPUAction = new HspDeletePUsByVersionCustomAction();
            actionSet.addAction(delPUAction, HspActionSet.CUSTOM, member);
        } else if (member.getObjectType() == HspConstants.gObjType_DPDimMember && member instanceof HspDPMember) {
            HspDPDimension dpDim = getDPDimension(member.getDimId());
            if (dpDim != null) {
                HspPMSVDimBinding pmDimBinding = hspPMDB.getPMSVDimBinding(dpDim.getScenarioId(), ((HspDPMember)member).getVersionId(), member.getIdForCache(), sessionId);
                if (pmDimBinding != null) { // Delete the DP PUH on behalf of the application owner
                    hspPMDB.deletePMDimension(pmDimBinding.getPMDimId(), actionSet, appOwnerSessionId);
                }
            }
        }

        // finally add action to delete member
        // TODO: check what happens when we call delete member
        deleteMember(member.getDimId(), member.getId(), true, actionSet, true, sessionId);
    }

    private synchronized void deleteMember(int dimensionId, int id, boolean shouldDeletePUs, HspActionSet actionSet, int sessionId) throws Exception {
        deleteMember(dimensionId, id, shouldDeletePUs, actionSet, false, sessionId);
    }


    private synchronized void deleteMember(int dimensionId, int id, boolean shouldDeletePUs, HspActionSet actionSet, boolean byPassValidation, int sessionId) throws Exception {
        try {
            //actionSet.beginBatchItem();
            // NOTE: It is assumed that the only actions in the actionSet for this method are those generated by the
            // deleteMembers method(s) only, and the actionSet does not contain any actions other than those associated
            // with a delete operation. See note below regarding the deletion of shared members for reason.
            //System.out.println("testDeleteMember - dimensionId: "+dimensionId+" id: "+id);
            HspDimension dim = getDimRoot(dimensionId);
            if (dim == null)
                throw new IllegalArgumentException("Invalid Dimension: " + dimensionId);
            // make sure object type checks uses this as opposed to getObjectType - necessitated for shared members
            HspMember member = getDimMember(dimensionId, id);
            if (member == null) // check parent
                throw new RuntimeException("Invalid member id: " + id);


            // Bug 281381: Validate the delete before deleting the planning units and other related members.
            if (!byPassValidation) {
                validateDelete(member, sessionId);
            }
            HashSet<Integer> pmDimIdsOfGeneratedPMActions = null;
            int objectType;
            if (member.isSharedMember()) {
                // If a delete operation deletes a subtree with a shared member and its corresponding base, this method
                // will be called twice: once for delete-all-shared-members-of-base, and; once for the shared member
                // itself. To prevent adding another delete action for this member and generate an optomisiticConcurreny
                // exception, we just return if a delete action for the shared member already exists in the actionSet.
                HspObject o = (HspObject)actionSet.getCachedObject(member);
                if (o != null)
                    return;
                // During a delete, allow the delete of a shared member to go
                // through even if the base member cannot be found.
                HspMember baseMember = getBaseMember(member, actionSet);
                objectType = baseMember == null ? member.getBaseMemberObjectType() : baseMember.getObjectType();
            } else {
                objectType = member.getObjectType();
                // Since the member is not shared, delete all of its shared members first
                // Since delete is currently not supported in the same transaction as an update, use
                // the non actionset version of getSharedMembersOfBase as the actionset version performs
                // poorly when the transaction size is > 200 which can be common for delete subtree.
                //Vector<HspMember> sharedMembers = this.getSharedMembersOfBase(dimensionId, id, actionSet, sessionId);
                Vector<HspMember> sharedMembers = this.getSharedMembersOfBase(dimensionId, id, sessionId);
                if (sharedMembers != null) {
                    for (int loop1 = 0; loop1 < sharedMembers.size(); loop1++) {
                        HspMember sharedMember = sharedMembers.elementAt(loop1);
                        if (sharedMember != null) {
                            deleteMember(sharedMember.getDimId(), sharedMember.getId(), shouldDeletePUs, actionSet, sessionId);
                        }
                    }
                }

                //            // update process management
                //            List<HspPlanningUnit> pus = hspPMDB.getPlanningUnitsForMember(member, sessionId);
                //            for (HspPlanningUnit pu : pus)
                //            {
                //                HspPlanningUnit tempPU = (HspPlanningUnit)actionSet.getCachedObject(pu);
                //                if (tempPU != null)
                //                    pu = tempPU;
                //
                //                //This tree only needs to be updated if the planning unit was started
                //                if ((pu != null) && (pu.getProcessState() != HspConstants.PU_NOT_STARTED))
                //                {
                //                    if (shouldDeletePUs)
                //                    {
                //                        hspPMDB.changePUStatus( pu.getScenarioId(), pu.getVersionId(), pu.getEntityId(), pu.getSecondaryMemberId(), HspConstants.PM_ACTION_ACTION_ID_EXCLUDE, 0, null, null, actionSet, sessionId);
                //                    }
                //                    else
                //                    {
                //                        throw new MemberUsedInPlanningUnitException(member.getName());
                //                    }
                //                }
                //            }

                // update process managememt if removing an entity
                //			if (member.getDimId() == HspConstants.kDimensionEntity)
                //			{
                //				HspEntity entity = (HspEntity) member;
                //				Vector<HspScenario> scenarios = getDimMembers(HspConstants.kDimensionScenario, false, sessionId);
                //				Vector<HspVersion> versions = getDimMembers(HspConstants.kDimensionVersion, false, sessionId);
                //
                //				if ((scenarios != null) && (versions != null))
                //				{
                //					//HspPUAction puAction = new HspPUAction();
                //					for (int loop1=0;loop1<scenarios.size();loop1++)
                //					{
                //						for (int loop2=0;loop2<versions.size();loop2++)
                //						{
                //							HspScenario scenario = scenarios.elementAt(loop1);
                //							HspVersion version = versions.elementAt(loop2);
                //							if ((scenario != null) && (version != null))
                //							{
                //                                // fetch the planning unit out of the cache
                //                                HspPlanningUnit pu = hspPMDB.getPlanningUnit(scenario.getId(), version.getId(), entity.getId());
                //                                // But if the planning unit is in the action set it's been updated within this transaction
                //                                // so reference that copy for changes
                //                                if (pu != null)
                //                                {
                //                                    HspPlanningUnit tempPU = (HspPlanningUnit)actionSet.getCachedObject(pu);
                //                                    if (tempPU != null)
                //                                    {
                //                                        pu = tempPU;
                //                                    }
                //                                 }
                //
                //								//This tree only needs to be updated if the planning unit was started
                //								if ((pu != null) && (pu.getProcessState() != HspConstants.PU_NOT_STARTED))
                //								{
                //                                    if (shouldDeletePUs)
                //                                    {
                //                                        hspPMDB.changePUStatusInternal(scenario.getMemberName(), version.getMemberName(), entity.getMemberName(), HspConstants.PM_ACTION_ACTION_ID_EXCLUDE, 0, null, null, actionSet, sessionId);
                //                                    }
                //                                    else
                //                                    {
                //                                        throw new MemberUsedInPlanningUnitException(entity.getName());
                //                                    }
                //								}
                //							}
                //						}
                //					}
                //				}
                //			}
                //////
            }

            if (isNotMemberObjectType(objectType)) // check object type
                throw new RuntimeException("Invalid Object Type (" + objectType + ") for this delete operation.");


            // Now delete the dimension (root node) itself
            // Currently only attribute dimension (root nodes) are deleted
            if (member.getId() == member.getDimId()) {
                if (member.isAttributeMember()) {
                    HspAttributeDimension dimension = getAttributeDimension(member.getId());
                    HspAction action = createAction(HspConstants.gObjType_AttributeDim);
                    actionSet.addAction(action, HspActionSet.DELETE, dimension);
                } else if (member.isDPMember()) {
                    HspAction action = createAction(HspConstants.gObjType_DPDimension); //TODO should this be allowed?
                    actionSet.addAction(action, HspActionSet.DELETE, dim);
                } else {
                    throw new RuntimeException("Delete Dimension not supported for this dimension type.");

                }
            } else {
                //System.out.println("adding delete action for: "+member.getName());
                HspAction action = createAction(objectType);
                //If the member is currency, check if predefined
                //if predefined, override the original record with predefined currency type
                if (member.getObjectType() == HspConstants.gObjType_Currency || (member.getObjectType() == HspConstants.gObjType_SimpleCurrency && member.getParentId() == HspSimpleCurrencyUtils.MEMBER_ID_HSP_INPUT_CURRENCIES)) {

                    HspCurrency currency = (HspCurrency)member;
                    HspCurrency preDefinedCurrencyUpd = hspCurDB.getOverriddenCurrency(currency.getCurrencyCode());
                    if (preDefinedCurrencyUpd != null) {
                        HspCurrency preDefinedCurrency = (HspCurrency)preDefinedCurrencyUpd.cloneForUpdate();
                        preDefinedCurrency.setCurrencyType(HspConstants.gintCurTypePreDefined);
                        actionSet.addAction(action, HspActionSet.UPDATE, preDefinedCurrency);
                    }
                }

                //            List<HspPMPrimaryMember> primaries = getPMPrimaryMembers(member.getId(), sessionId);
                //            if (primaries != null)
                //                for (HspPMPrimaryMember primary : primaries)
                //                {
                //                    HspPMDimDef dimDef = hspPMDB.getPMDimDef(primary.getPMDimId(), sessionId);
                //                    primary = (HspPMPrimaryMember) primary.cloneForUpdate();
                //                    primary.setEnabledForPm(false);
                //                    primary.setSecondaryDimId(0);
                //                    primary.setSecondaryAscendantId(0);
                //                    primary.setAllowedSecondaryRelGens(null);
                //                    primary.setAutoEnabledByRelGen(false);
                //
                //                    dimDef.setSecondaryMembers(primary, null);
                //                    dimDef.updatePrimaryMember(primary);
                //
                //                    hspPMDB.savePMDimDef(dimDef, actionSet, sessionId);
                //                }
                //
                //            List<HspPMSecondaryMember> secondaries = hspPMDB.getPMSecondaryMembersBySecondaryId(member.getId(), sessionId);
                //            if (secondaries != null)
                //                for (HspPMSecondaryMember secondary : secondaries)
                //                {
                //                    HspPMPrimaryMember primary = hspPMDB.getPMPrimaryMember(secondary.getPMDimId(), secondary.getPrimaryMemberId(), sessionId);
                //                    HspPMPrimaryMember tempPrimary = (HspPMPrimaryMember)actionSet.getCachedObject(primary);
                //                    if (tempPrimary != null)
                //                        primary = tempPrimary;
                //
                //                    HspPMDimDef dimDef = hspPMDB.getPMDimDef(primary.getPMDimId(), sessionId);
                //                    List<HspMember> secMembersList = dimDef.getSecondaryMembers(primary);
                //                    secMembersList = new ArrayList<HspMember>(secMembersList.subList(0, secMembersList.size()));
                //                    HspMember sec2del = getDimMember(primary.getSecondaryDimId(), secondary.getSecondaryMemberId());
                //                    secMembersList.remove(sec2del);
                //                    dimDef.setSecondaryMembers(primary, secMembersList);
                //                    hspPMDB.savePMDimDef(dimDef, actionSet, sessionId);
                //                }

                //pmDimIdsOfGeneratedPMActions = createAncillaryPMActions(member, 0, HspActionSet.DELETE, actionSet, sessionId);

                HspMemberInfo memberInfo = new HspMemberInfo(member, member, sessionId);
                deleteMemberAliases(actionSet, member, memberInfo, sessionId);
                deleteMemberAttributeBindings(actionSet, member.getId(), member.getDimId(), memberInfo);
                deleteMemberUDABindings(actionSet, member.getId(), member.getDimId(), memberInfo);

                if (member.getDimId() == HspConstants.kDimensionVersion && ((HspVersion)member).isSandboxVersion()) {
                    //Add sandbox audit history delete as well
                    HspAction sbAction = new HspSandboxAuditHistoryAction();
                    HspSandboxAuditHistory hspSandboxHistory = new HspSandboxAuditHistory();
                    hspSandboxHistory.setSandboxId(member.getId());
                    actionSet.addAction(sbAction, HspActionSet.DELETE, hspSandboxHistory);
                }
                actionSet.addAction(action, HspActionSet.DELETE, member);
                actionSet.addAction(new HspMemberInfoAction(), HspActionSet.DELETE, memberInfo);

                executeMemberCallbackRules((memberInfo != null ? memberInfo.getMember() : member), (memberInfo != null ? memberInfo.getOldMember() : null), actionSet, ActionMethod.DELETE,
                                           PBCSPreAndPostScriptFactory.getInstance().getScriptNames(hspJS.getSystemCfg().getApplicationType(), HspPreAndPostOperationType.POST_MEMBER_QUEUE_FOR_DELETE), sessionId);

                //Cache will be automatically updated/invalidated by the HspAction class
            }

        } finally {
            //actionSet.endBatchItem();
        }
    }

    public synchronized void swapMember(HspMember member, boolean moveQualifier, int sessionId) throws Exception {
        if (member != null) {
            Vector children = null;
            //HspMember parent = (HspMember)member.getParent();
            HspMember parent = this.getDimMember(member.getDimId(), member.getParentId());
            HspMember tempMember = null;
            if (parent != null) {
                children = parent.getChildren();
            }


            //if move-up get the previous element
            if (moveQualifier) {
                if ((children != null) && (children.size()) > 0) {
                    for (int i = 0; i < children.size(); i++) {
                        tempMember = (HspMember)children.elementAt(i);
                        if (tempMember.getId() == member.getId()) {
                            if ((i > 0) && (i < children.size())) {
                                tempMember = (HspMember)children.elementAt(i - 1);
                            } else {
                                tempMember = null;
                            }
                            break;
                        }
                    }
                }
            }
            //if move-down get the next element
            else {
                if ((children != null) && (children.size()) > 0) {
                    for (int i = 0; i < children.size(); i++) {
                        tempMember = (HspMember)children.elementAt(i);
                        if (tempMember.getId() == member.getId()) {
                            if ((i >= 0) && (i < children.size() - 1)) {
                                tempMember = (HspMember)children.elementAt(i + 1);
                            } else {
                                tempMember = null;
                            }
                            break;
                        }
                    }
                }
            } //else
            //now swap
            if (tempMember != null) {
                swapMember(member, tempMember, moveQualifier, sessionId);
            }
        }
    }

    private synchronized void swapMember(HspMember swapThis, HspMember swapWith, boolean moveQualifier, int sessionId) throws Exception {
        if ((swapThis != null) && (swapWith != null)) {
            double swapThisPos = swapThis.getPosition();
            double swapWithPos = swapWith.getPosition();

            //swap member
            HspMember swapThisClone = null;
            HspMember swapWithClone = null;

            //moveup: set the predecessor (with) to this member's position
            if (moveQualifier) {
                // Look up the newest version of this member so that the
                // member always contains the PTProps
                // See Bug 24347178 - PBCS: MOVE MEMBERS LOOSE THE MEMBER FORMULA
                // See Bug 24600311 - AFTER THE UPGRADE FORMULAS DISSAPEAR
                swapWithClone = getDimMember(swapWith.getDimId(), swapWith.getId());
                if (swapWithClone == null)
                    throw new IllegalStateException("Cound not retrieve member for swapMember: " + swapWith);
                swapWithClone = (HspMember)swapWithClone.cloneForUpdate();
                swapWithClone.setPosition(swapThisPos);
            }
            //movedown - set this member with the successor's (with's) position
            else {
                // Look up the newest version of this member so that the
                // member always contains the PTProps
                // See Bug 24347178 - PBCS: MOVE MEMBERS LOOSE THE MEMBER FORMULA
                // See Bug 24600311 - AFTER THE UPGRADE FORMULAS DISSAPEAR
                swapThisClone = getDimMember(swapThis.getDimId(), swapThis.getId());
                if (swapThisClone == null)
                    throw new IllegalStateException("Cound not retrieve member for swapMember: " + swapThis);
                swapThisClone = (HspMember)swapThisClone.cloneForUpdate();
                swapThisClone.setPosition(swapWithPos);
            }
            //swap
            //swapThisClone.setPosition(swapWithPos);
            //swapWithClone.setPosition(swapThisPos);


            //open a transaction and save
            HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
            HspActionSet actionSet = new HspActionSet(hspSQL, user);
            //moveup: set the predecessor (with) to this member's position
            if (moveQualifier) {
                saveMember(swapWithClone, actionSet, sessionId);
            }
            //movedown - set this member with the successor's (with's) position
            else {
                saveMember(swapThisClone, actionSet, sessionId);
            }
            actionSet.doActions();
        }
    }

    public synchronized void saveDimensions(List dimensionsList, int sessionId) throws Exception {
        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        for (int i = 0; i < dimensionsList.size(); i++) {
            HspDimension dim = (HspDimension)dimensionsList.get(i);
            saveDimension(dim, actionSet, sessionId);
        }
        actionSet.doActions();
    }

    public synchronized void saveDimension(HspDimension dimension, int sessionId) throws Exception {
        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        boolean weAreAdding = isNewDimension(dimension, sessionId);
        validateDimensionForSeededCubes(dimension, sessionId, weAreAdding);

        //If seeded member & the user is not module user, track the changes
        boolean isObjectSeeded = hspJS.getFeatureDB(sessionId).isObjectSeeded(dimension.getId(), dimension.getObjectType());
        boolean shouldTrack = HspModuleInfo.getModuleUser() == null || (!HspModuleInfo.getModuleUser().equals(HspModuleInfo.MODULE_USER));
        List<HspDiff> diffList = null;
        if (isObjectSeeded && shouldTrack) {
            ReadOnlyCachedObject objInCache = hspJS.getFeatureDB(sessionId).getPlanningObject(dimension.getName(), dimension.getObjectType(), sessionId);
            diffList = dimension.getDiffList(hspJS, objInCache, sessionId);
        }

        //Next we execute the action set
        saveDimension(dimension, actionSet, sessionId);

        if (isObjectSeeded && shouldTrack && diffList != null && diffList.size() > 0) {
            int artifactId = hspJS.getFeatureDB(sessionId).getModuleArtifactDetailByObjId(dimension.getId(), dimension.getObjectType()).getId();
            // TODO: Verify the next line handles the rollback of the actionSet as this method maybe retried multiple times
            hspJS.getFeatureDB(sessionId).addAuditRecord(HspActionSet.UPDATE, diffList, artifactId, sessionId);
        }

        if (!weAreAdding) {
            //Update Exchange Rates form only if custom dimension is enabled/disabled for Plan1.
            updateXchgeRateForm(dimension, sessionId, weAreAdding);
        }

        actionSet.doActions();
        //Cache will be automatically updated/invalidated by the HspAction class
        //update dimension for simple currency
        if (weAreAdding) {
            updateDimensionPostSave(dimension, sessionId);
        }
    }

    public synchronized void saveDimension(HspDimension dimension, HspActionSet actionSet, int sessionId) throws Exception {
        logger.entering(dimension);
        // generic dimension setting and checks
        if (dimension == null)
            throw new RuntimeException("Invalid dimension: null");

        // TODO: Remove the following validation post 800
        validateFeatureIsEnabled(dimension);

        // Get out if we're trying to add/modify unsupported dimension type for this method
        // Currently only supporing accounts, entities, user defined, time and attribute dimensions
        switch (dimension.getDimType()) {
        case HspConstants.kDimTypeAccount:
            break;
        case HspConstants.kDimTypeAttribute:
            break;
        case HspConstants.kDimTypeEntity:
            break;
        case HspConstants.kDimTypeUser:
            break;
        case HspConstants.kDimTypeTime:
            break;
        case HspConstants.kDimTypeDPDimension:
            break;
        case HspConstants.kDimTypeReplacementDimension:
            break;
        case HspConstants.kDimTypeMetric:
            break;
            //case HspConstants.kDimTypeBRDimension: break;
        default:
            throw new RuntimeException("Unsupported dimension type for this operation.");


        }
        //if ( !((dimension.isDimEditor()) || (dimension.getDimType() == HspConstants.kDimTypeAttribute)))
        //	throw new RuntimeException("Unsupported dimension type for this operation.");


        // Check the dimension type and the object type to make sure they're supported AND compatible
        switch (dimension.getObjectType()) {
            // getObjectType() for attribute dimension returns attribute member type
        case HspConstants.gObjType_AttributeMember:
            if (dimension.getDimType() != HspConstants.kDimTypeAttribute)
                throw new RuntimeException("Dimension Type: " + dimension.getDimType() + " and Object Type: " + dimension.getObjectType() + " are incompatible for dimension: " + dimension.getName());
            break;
        case HspConstants.gObjType_DPDimMember:
            if (dimension.getDimType() != HspConstants.kDimTypeDPDimension)
                throw new RuntimeException("Dimension Type: " + dimension.getDimType() + " and Object Type: " + dimension.getObjectType() + " are incompatible for dimension: " + dimension.getName());
            break;
        case HspConstants.gObjType_Account:
            if (dimension.getDimType() != HspConstants.kDimTypeAccount)
                throw new RuntimeException("Dimension Type: " + dimension.getDimType() + " and Object Type: " + dimension.getObjectType() + " are incompatible for dimension: " + dimension.getName());
            break;
        case HspConstants.gObjType_Entity:
            if (dimension.getDimType() != HspConstants.kDimTypeEntity)
                throw new RuntimeException("Dimension Type: " + dimension.getDimType() + " and Object Type: " + dimension.getObjectType() + " are incompatible for dimension: " + dimension.getName());
            break;
        case HspConstants.gObjType_UserDefinedMember:
            if (dimension.getDimType() != HspConstants.kDimTypeUser)
                throw new RuntimeException("Dimension Type: " + dimension.getDimType() + " and Object Type: " + dimension.getObjectType() + " are incompatible for dimension: " + dimension.getName());
            break;
        case HspConstants.gObjType_Scenario:
            if (dimension.getDimType() != HspConstants.kDimTypeUser)
                throw new RuntimeException("Dimension Type: " + dimension.getDimType() + " and Object Type: " + dimension.getObjectType() + " are incompatible for dimension: " + dimension.getName());
            break;
        case HspConstants.gObjType_Version:
            if (dimension.getDimType() != HspConstants.kDimTypeUser)
                throw new RuntimeException("Dimension Type: " + dimension.getDimType() + " and Object Type: " + dimension.getObjectType() + " are incompatible for dimension: " + dimension.getName());
            break;
        case HspConstants.gObjType_Dimension:
            if (dimension.getDimType() != HspConstants.kDimTypeUser)
                throw new RuntimeException("Dimension Type: " + dimension.getDimType() + " and Object Type: " + dimension.getObjectType() + " are incompatible for dimension: " + dimension.getName());
            break;
        case HspConstants.gObjType_Currency:
            if (dimension.getDimType() != HspConstants.kDimTypeUser)
                throw new RuntimeException("Dimension Type: " + dimension.getDimType() + " and Object Type: " + dimension.getObjectType() + " are incompatible for dimension: " + dimension.getName());
            break;
        case HspConstants.gObjType_Period:
            if (dimension.getDimType() != HspConstants.kDimTypeTime)
                throw new RuntimeException("Dimension Type: " + dimension.getDimType() + " and Object Type: " + dimension.getObjectType() + " are incompatible for dimension: " + dimension.getName());
            break;
        case HspConstants.gObjType_Year:
            if (dimension.getDimType() != HspConstants.kDimTypeUser)
                throw new RuntimeException("Dimension Type: " + dimension.getDimType() + " and Object Type: " + dimension.getObjectType() + " are incompatible for dimension: " + dimension.getName());
            break;
        case HspConstants.gObjType_BudgetRequest:
            if (dimension.getDimType() != HspConstants.kDimTypeUser)
                throw new RuntimeException("Dimension Type: " + dimension.getDimType() + " and Object Type: " + dimension.getObjectType() + " are incompatible for dimension: " + dimension.getName());
            break;
        case HspConstants.gObjType_DecisionPackageASO:
            if (dimension.getDimType() != HspConstants.kDimTypeUser)
                throw new RuntimeException("Dimension Type: " + dimension.getDimType() + " and Object Type: " + dimension.getObjectType() + " are incompatible for dimension: " + dimension.getName());
            break;
        case HspConstants.gObjType_BudgetRequestASO:
            if (dimension.getDimType() != HspConstants.kDimTypeUser)
                throw new RuntimeException("Dimension Type: " + dimension.getDimType() + " and Object Type: " + dimension.getObjectType() + " are incompatible for dimension: " + dimension.getName());
            break;
        case HspConstants.gObjType_SimpleCurrency:
            if (dimension.getDimType() != HspConstants.kDimTypeUser)
                throw new RuntimeException("Dimension Type: " + dimension.getDimType() + " and Object Type: " + dimension.getObjectType() + " are incompatible for dimension: " + dimension.getName());
            break;
        case HspConstants.gObjType_ReplacementDimension:
        case HspConstants.gObjType_ReplacementMember:
            if (dimension.getDimType() != HspConstants.kDimTypeReplacementDimension)
                throw new RuntimeException("Dimension Type: " + dimension.getDimType() + " and Object Type: " + dimension.getObjectType() + " are incompatible for dimension: " + dimension.getName());
            break;
        case HspConstants.gObjType_Metric:
            if (dimension.getDimType() != HspConstants.kDimTypeMetric)
                throw new RuntimeException("Dimension Type: " + dimension.getDimType() + " and Object Type: " + dimension.getObjectType() + " are incompatible for dimension: " + dimension.getName());
            break;
        default:
            throw new RuntimeException("Unsupported Object Type: " + dimension.getObjectType() + " for operation; dimension: " + dimension.getName());


        }
        // figure out whether we're adding or updating. If oldDinemsion == null we're adding
        // if oldDImension != null we're upating or modifying an existing dimension
        // S.J - Exception in case of HSP_RATES. If it is HSP_RATES, then
        // we are not adding. The call comes in for performance Settings
        HspDimension oldDimension = getDimRoot(dimension.getDimId());
        boolean weAreAdding = true;
        if (dimension.getDimId() == HspConstants.kDimensionRates) {
            weAreAdding = false;
        } else {
            weAreAdding = (oldDimension == null);
            if (weAreAdding && getNumDimensions(sessionId) >= HspConstants.MAX_NUM_DIMENSIONS && shouldDimensionCount(dimension)) {
                throw new HspRuntimeException("MSG_MAX_DIMS_REACHED");
            }
        }

        // validate the dimension name
        validateDimensionMemberName(dimension.getName());

        // validate the description. no trimming done: responisiblity of the callers
        validateObjectDescription(dimension.getName());

        // validate any aliases specified
        validateAliases(dimension);

        Object[][] aliases = dimension.getAliasesToBeSaved();
        dimension.setAliasesToBeSaved(null);


        // defered depending on type -> dimension.setObjectType(HspConstants.gObjType_Dimension);
        // defered depending on type -> dimension.setParentId(HspConstants.gFolder_Dimensions);
        if (weAreAdding) {
            dimension.setHasChildren(false);
            // set the position. If it's non-zero leave it as is
            if (dimension.getPosition() == 0.0)
                dimension.setPosition(getNextDimensionPosition(dimension, sessionId));
            //TODO: adjust position of other dimensions if not at end
        }
        // validate data storage and consolidation op's
        validateDataStorage(dimension);
        // dims can't have consolidation op's
        for (int plan = 1; plan <= HspConstants.PLAN_TYPE_ALL; plan = plan << 1) {
            // rather than returning an error jsut set consol_op to ignore
            if (dimension.getConsolOp(plan) != HspConstants.kDataConsolIgnore)
                dimension.setConsolOp(plan, HspConstants.kDataConsolIgnore);
            //throw  new RuntimeException("Consolidtaion operator for all plan types on root dimension members must be set to Ignore.");
        }

        if (dimension.isUsedForConsol())
            throw new RuntimeException("Dimension root members can not be used for consolidation"); // user set


        // two pass calc must be false for dimensions, even if they don't use it.
        // took this check out for 9.3.1
        //if (dimension.getTwopassCalc())
        //	throw  new RuntimeException("Two Pass Calculation must be set to false for root dimension members.");

        // check has member FX
        if (dimension.hasMbrFx())
            throw new RuntimeException("Has Member Exchange Rate must be set to false for root dimension members.");


        // Validate plan types specifed for the dimension. They must be in the range of all possible
        // and a subset of those defined for the application.
        // dimension.setusedin what do we do here, no set prop on dimension, lastsavedusedin needed?
        int dimensionPlanTypes = dimension.getUsedIn();
        if (!HspMember.areDefinedPlanTypes(dimensionPlanTypes))
            throw new RuntimeException("Invalid Plan Types specified for dimension: " + dimension.getName() + ": " + dimensionPlanTypes);


        HspSystemCfg cfg = hspJS.getSystemCfg();
        int applicationPlanTypes = cfg.getPlanTypes();
        if ((dimensionPlanTypes & applicationPlanTypes) != dimensionPlanTypes)
            throw new IllegalArgumentException("Invalid Plan Types specified for dimension: " + dimension.getName() + ". Not a subset of application Plan Types.");


        //TODO: may need to update the kiddies here if plan types change a certain way...

        // source plan type checking only for dim's that use it, e.g. accounts
        // The plantypes have been checked for application so if it is a root member
        // we need not verify the plantypes and sourceplantypes again
        if (!dimension.isRootMember())
            validatePlanTypesAndSourcePlanType(dimension);

        // dimension.getEnforceSecurity(); //

        /* density property is obsolete, superceded by density1, 2, 4
		if  (!((dimension.getDensity() == HspConstants.kDataDensityDense) ||
			   (dimension.getDensity() == HspConstants.kDataDensitySparse))) // density on dimension
				throw new RuntimeException("Invalid Density value for dimension: "+dimension.getName());
		*/

        // density 1,2,4
        validateDensitySettings(dimension);

        // position 1,2,4
        if (weAreAdding)
            dimension.setPosition(getNextDimensionPosition(dimension, sessionId));
        else
            ; //TODO: need to implement position calculations
        // see updateChildMembers and getNextDimensionPosition for way to do it

        HspDimensionAction action;
        if (weAreAdding) {
            switch (dimension.getDimType()) // << on dimension, set to HspConstants.kDimTypeUser
            {
            case HspConstants.kDimTypeUser:
                if (dimension instanceof HspAttributeDimension)
                    throw new RuntimeException("Dimension type HspDimension != instance type HspAttributeDimension");


                validateUserDefinedDimensionForAdd(dimension, sessionId);
                action = new HspDimensionAction();
                actionSet.addAction(action, HspActionSet.ADD, dimension);
                actionSet.doActions(false);
                updateMemberAliases(actionSet, dimension, aliases, false, sessionId);
                break;
            case HspConstants.kDimTypeDPDimension:
                if (!(dimension instanceof HspDPDimension))
                    throw new RuntimeException("Dimension type HspDPDimension != instance type HspDimension");


                action = new HspDPDimensionAction();
                actionSet.addAction(action, HspActionSet.ADD, dimension);
                updateMemberAliases(actionSet, dimension, aliases, true, sessionId);
                break;
            case HspConstants.kDimTypeReplacementDimension:
                if (!(dimension instanceof HspReplacementDimension))
                    throw new RuntimeException("Dimension type HspReplacementDimension != instance type HspDimension");


                action = new HspReplacementDimensionAction();
                actionSet.addAction(action, HspActionSet.ADD, dimension);
                updateMemberAliases(actionSet, dimension, aliases, true, sessionId);
                break;
            case HspConstants.kDimTypeAttribute:
                if (!(dimension instanceof HspAttributeDimension))
                    throw new RuntimeException("Dimension type HspAttributeDimension != instance type HspDimension");


                HspAttributeDimension aDimension = (HspAttributeDimension)dimension;
                HspDimension baseDim = getDimRoot(aDimension.getBaseDimId());
                //MSG_INVALID_DIMID_FOR_MEMBER
                if (baseDim == null) {
                    Properties p = new Properties();
                    p.put("DIM", aDimension.getName());
                    throw new HspRuntimeException("MSG_ADM_UNK_BASEDIM", p);
                }
                if (!supportsAttributes(aDimension))
                    throw new HspRuntimeException("MSG_ATTRIBUTES_ON_SPARSE_ONLY");


                validateAttributeDimensionForAdd(aDimension);
                // Ensure that the baseDimension has not changed since the validation took place.
                actionSet.addVerifyTimestampAction(baseDim);

                action = new HspAttributeDimensionAction();
                actionSet.addAction(action, HspActionSet.ADD, aDimension);
                break;
            default:
                throw new RuntimeException("Adding a dimension of type " + dimension.getDimType() + " is not supported. Dimension Name: " + dimension.getName());


            }
        } else // we're modifying
        {
            switch (dimension.getDimType()) // << on dimension, set to HspConstants.kDimTypeUser
            {
            case HspConstants.kDimTypeUser:
                if (dimension instanceof HspAttributeDimension)
                    throw new RuntimeException("Dimension type HspDimension != instance type HspAttributeDimension");


                validateUserDefinedDimensionForUpdate(dimension);
                action = new HspDimensionAction();
                actionSet.addAction(action, HspActionSet.UPDATE, dimension);
                updateMemberAliases(actionSet, dimension, aliases, true, sessionId);
                break;
            case HspConstants.kDimTypeAttribute:
                if (!(dimension instanceof HspAttributeDimension))
                    throw new RuntimeException("Dimension type HspAttributeDimension != instance type HspDimension");


                HspAttributeDimension aDimension = (HspAttributeDimension)dimension;
                HspAttributeDimension oDimension = getAttributeDimension(oldDimension.getId());
                HspDimension baseDim = getDimRoot(aDimension.getBaseDimId());
                if (baseDim == null) {
                    Properties p = new Properties();
                    p.put("DIM", aDimension.getName());
                    throw new HspRuntimeException("MSG_ADM_UNK_BASEDIM", p);
                }


                validateAttributeDimensionForUpdate(oDimension, aDimension);
                // Ensure that the baseDimension has not changed since the validation took place.
                actionSet.addVerifyTimestampAction(baseDim);
                action = new HspAttributeDimensionAction();
                actionSet.addAction(action, HspActionSet.UPDATE, aDimension);
                break;
                // TODO validation functions for adding/editing other dim types for updation
                // These need not be there in add as adding account, entity and time root dimension
                // is not supported
            case HspConstants.kDimTypeDPDimension:
                if (!(dimension instanceof HspDPDimension))
                    throw new RuntimeException("Dimension type HspDPDimension != instance type HspDimension");


                action = new HspDPDimensionAction();
                actionSet.addAction(action, HspActionSet.UPDATE, dimension);
                updateMemberAliases(actionSet, dimension, aliases, true, sessionId);
                break;
            case HspConstants.kDimTypeAccount:
                //validateUserDefinedDimensionForUpdate(dimension);
                action = new HspDimensionAction();
                actionSet.addAction(action, HspActionSet.UPDATE, dimension);
                updateMemberAliases(actionSet, dimension, aliases, true, sessionId);
                break;
            case HspConstants.kDimTypeEntity:
                //validateAttributeDimensionForUpdate(oDimension, aDimension);
                action = new HspDimensionAction();
                actionSet.addAction(action, HspActionSet.UPDATE, dimension);
                updateMemberAliases(actionSet, dimension, aliases, true, sessionId);
                break;
            case HspConstants.kDimTypeTime:
                //validateAttributeDimensionForUpdate(oDimension, aDimension);
                action = new HspDimensionAction();
                actionSet.addAction(action, HspActionSet.UPDATE, dimension);
                updateMemberAliases(actionSet, dimension, aliases, true, sessionId);
                break;
            case HspConstants.kDimTypeMetric:
                if (!(HspDimension.class.equals(dimension.getClass())))
                    throw new RuntimeException("Dimension type HspMetric != instance type HspDimension");
                action = new HspDimensionAction();
                actionSet.addAction(action, HspActionSet.UPDATE, dimension);
                updateMemberAliases(actionSet, dimension, aliases, true, sessionId);
                break;
            case HspConstants.kDimTypeReplacementDimension:
                if (!(dimension instanceof HspReplacementDimension))
                    throw new RuntimeException("Dimension type HspReplacementDimension != instance type HspDimension");
                action = new HspReplacementDimensionAction();
                actionSet.addAction(action, HspActionSet.UPDATE, dimension);
                updateMemberAliases(actionSet, dimension, aliases, true, sessionId);
                break;
            default:
                throw new RuntimeException("modifying a dimension of type " + dimension.getDimType() + " is not supported. Dimension Name: " + dimension.getName());


            }

            if (dimension.getDimId() == HspConstants.gObjDim_BudgetRequest && dimension.getUsedIn() != dimension.getLastSavedUsedIn()) {
                HspMember parent = getDimMember(dimension.getDimId(), dimension.getDimId());
                int parentUsedIn = dimension.getUsedIn();
                Vector<HspMember> descendants = parent.getDescendants(false, false);
                if (descendants != null)
                    for (int i = 0; i < descendants.size(); i++) {
                        HspMember child = descendants.get(i);
                        child = (HspMember)child.cloneForUpdate();
                        child.setUsedIn(parentUsedIn);
                        updateMember(child, actionSet, null, sessionId);
                    }
            }
            // No need to process HSP_Rates dim as members are not stored in relational
            if (dimension.getDimId() != HspConstants.gObjDim_Rates && dimension.getDimId() != HspConstants.gObjDim_BudgetRequest && dimension.getUsedIn() != dimension.getLastSavedUsedIn() && (dimension.getUsedIn() & dimension.getLastSavedUsedIn()) == dimension.getUsedIn()) {
                HspMember parent = getDimMember(dimension.getDimId(), dimension.getDimId());

                Vector<HspMember> dimChildren = parent.getChildren();
                int parentUsedIn = dimension.getUsedIn();

                if (dimChildren != null)
                    for (int i = 0; i < dimChildren.size(); i++) {
                        HspMember child = dimChildren.get(i);
                        // HspMember tempMbr = (HspMember)actionSet.getCachedObject(child);
                        // if (tempMbr != null)
                        //     continue;
                        if ((parentUsedIn & child.getUsedIn()) != child.getUsedIn()) {
                            child = (HspMember)child.cloneForUpdate();
                            child.setUsedIn(deriveValidPlanType(child.getUsedIn(), parentUsedIn));
                            if (child.isObjectLocked()) {
                                child.setOverrideLock(true);
                            }
                            if (dimension.getDimId() == HspConstants.gObjDim_Account && ((((HspAccount)child).getSrcPlanType() & parentUsedIn) != ((HspAccount)child).getSrcPlanType())) {
                                setSourcePlanType((HspAccount)child);
                            }
                            updateMember(child, actionSet, null, sessionId);
                        }
                    }
            }
            // update No member with dimension plan typr
            updateNoMemberForSimplifiedCurrency(dimension, actionSet, sessionId);
            updateVersionMembersForUpdatedPlanType(dimension, actionSet, sessionId);
            // If the dimension is the Period dimension and is enabled
            // for new plan types propagate the new plan type down to
            // its descendants.
            if (dimension.getDimId() == HspConstants.kDimensionTimePeriod) {
                int oldUsedIn = oldDimension == null ? 0 : oldDimension.getUsedIn();
                int addedUsedIn = dimension.getUsedIn() & ~oldUsedIn;
                if (addedUsedIn != 0) {
                    // we want to include DTS members when propagating dimension usedIn to members
                    List<HspMember> periods = (List<HspMember>)getDimMembers(dimension.getId(), false, true, sessionId);
                    if (HspUtils.size(periods) > 0) {
                        List<HspTimePeriod> updatedPeriods = new ArrayList<HspTimePeriod>();
                        for (HspMember member : periods) {
                            HspTimePeriod period = (HspTimePeriod)member;
                            CachedObject co = actionSet.getCachedObject(period);
                            if (co instanceof HspTimePeriod) {
                                period = (HspTimePeriod)co;
                                period.setUsedIn(HspUtils.setBitMask(period.getUsedIn(), addedUsedIn, true));
                                trackMemberChangesIfSeeded(period, sessionId);
                            } else if (!HspUtils.getBitMask(period.getUsedIn(), addedUsedIn)) {
                                period = (HspTimePeriod)period.cloneForUpdate();
                                period.setUsedIn(HspUtils.setBitMask(period.getUsedIn(), addedUsedIn, true));
                                trackMemberChangesIfSeeded(period, sessionId);
                                updatedPeriods.add(period);
                            }
                        }
                        if (updatedPeriods.size() > 0) {
                            actionSet.addAction(new HspTimePeriodAction(), HspActionSet.UPDATE, updatedPeriods.toArray(new HspTimePeriod[updatedPeriods.size()]));
                        }
                    }
                }
            }
        }

        logger.exiting();
    }

    private boolean supportsAttributes(HspAttributeDimension attrDim) {
        //return true;
        // TODO: re-enable check
        if (attrDim == null)
            throw new IllegalArgumentException("Attribute dimension cannot be null");

        if (!attrDim.isIndexed())
            return true;

        HspDimension dim = getDimRoot(attrDim.getBaseDimId());
        if (dim == null)
            return false;

        switch (dim.getDimId()) {
        case HspConstants.kDimensionScenario:
        case HspConstants.kDimensionVersion:
        case HspConstants.kDimensionTimePeriod:
        case HspConstants.kDimensionYear:
        case HspConstants.kDimensionCurrency:
            //                return (false);
        case HspConstants.kDimensionEntity:
        case HspConstants.kDimensionAccount:
            for (int planType = 1; planType <= HspConstants.PLAN_TYPE_ALL; planType = planType << 1) {
                if (((dim.getUsedIn() & planType) == planType) && (dim.getDensity(planType) != HspConstants.kDataDensitySparse))
                    return false;
            }
            return (true);
        default:
            for (int planType = 1; planType <= HspConstants.PLAN_TYPE_ALL; planType = planType << 1) {
                if (((dim.getUsedIn() & planType) == planType) && (dim.getDensity(planType) != HspConstants.kDataDensitySparse))
                    return false;
            }
            return (true);
        }
    }

    private boolean shouldDimensionCount(HspDimension dimension) {
        if (dimension == null || dimension.getDimType() == HspConstants.kDimTypeReplacementDimension || dimension.getDimType() == HspConstants.kDimTypeMetric)
            return false;

        // check for known dim Ids (skip rates dimension)
        if (dimension.getDimId() > HspConstants.kDimensionRates && dimension.getDimId() <= HspConstants.kDimensionLast)
            return true;

        // now check that we have a user defined dimension
        if (dimension.getDimType() == HspConstants.kDimTypeUser)
            return true;

        return false;
    }

    //	private synchronized void adjustDimensionPositionValues(HspDimension dimension, HspDimension oldDimension, HspActionSet actionSet, HspAction action, int sessionId)
    //	{
    //		// This method modifies the position1,2,4 properties of all dimensions affected by the
    //		// modification
    //
    //		// If the position values didn't change then just return
    //        boolean positionChanged = false;
    //        for (int plan = 1; plan <= HspConstants.PLAN_TYPE_ALL; plan = plan << 1)
    //        {
    //            if (oldDimension.getPosition(plan) != dimension.getPosition(plan))
    //            {
    //                positionChanged = true;
    //                break;
    //            }
    //        }
    //        if (!positionChanged)
    //            return;
    //
    //		// cannot modify attribute dimension positions from what they were when they were added,
    //		// (because position on attrib dims makes no sense) so if no attempt was made to change them throw an exception.
    //		if (dimension.getDimType() == HspConstants.kDimTypeAttribute)
    //			throw  new RuntimeException("Attribute dimension position properties can not be modified.");
    //
    //		// get the relevant dimensions whose positions may be affected by position changes of
    //		// the dimension being modified.
    //		Vector dimensions = getBaseDimensions(HspConstants.PLAN_TYPE_ALL, sessionId);
    //		if (dimensions == null)
    //			throw  new RuntimeException("Unable to obtain dimension information");
    //		for (int i=0;i<dimensions.size();i++)
    //		{
    //			HspDimension dim = (HspDimension)dimensions.elementAt(i);
    //			//Remove all dimensions that are null or are not dimension object types.
    //			if ((dim == null) || (dim.getObjectType() != HspConstants.gObjType_Dimension))
    //			{
    //				dimensions.removeElementAt(i);
    //				i--;
    //			}
    //		}
    //		// make sure that position values specified for modify are within range
    //		int dimMax = dimensions.size();
    //        for (int plan = 1; plan <= HspConstants.PLAN_TYPE_ALL; plan = plan << 1)
    //        {
    //            if ((dimension.getPosition(plan) < 1) || (dimension.getPosition(plan) > dimMax))
    //                throw  new RuntimeException("Position"+plan+" value is out of range for dimension: "+dimension.getName());
    //        }
    //
    //
    //		// get position information for each plan type
    //		// up: delta > 0;  down: delta < 0; no change: delta == 0
    //		// if (up): < old nothing; > old && <= new: -1
    //		// if (down): > old nothing;  >= new && < old: + 1
    //		int position1Delta = dimension.getPosition1() - oldDimension.getPosition1();
    //		//int position2Delta = dimension.getPosition2() - oldDimension.getPosition2();
    //		//int position4Delta = dimension.getPosition4() - oldDimension.getPosition4();
    //		for (int i=0; i<dimensions.size(); i++)
    //		{
    //			HspDimension uDimension = (HspDimension)((HspDimension)dimensions.elementAt(i)).cloneForUpdate();
    //			// don't modify the dimension that required position changes for all other dimensions
    //			// that's the responsibility of the caller.
    //			if (uDimension.getId() == dimension.getId())
    //				continue;
    //			if (position1Delta > 0)
    //			{
    //				if ((uDimension.getPosition1() > oldDimension.getPosition1()) && (uDimension.getPosition1() <= dimension.getPosition1()))
    //					uDimension.setPosition1(uDimension.getPosition1() - 1);
    //			}
    //			else if (position1Delta < 0)
    //			{
    //				if ((uDimension.getPosition1() < oldDimension.getPosition1()) && (uDimension.getPosition1() >= dimension.getPosition1()))
    //					uDimension.setPosition1(uDimension.getPosition1() + 1);
    //			}
    //			//else nothing to do
    //			//TODO: debug with above, and then replicate with each position when working
    //			uDimension = (HspDimension)uDimension.cloneForUpdate();
    //			actionSet.addAction(action, HspActionSet.UPDATE, uDimension);
    //		}
    //	}

    private double getNextDimensionPosition(HspDimension dimension, int sessionId) {
        // This method should only be called on add's as the position property (not position1,2,4)
        // is only set once on the initial add. And the position1,2,4 values are set to this once
        // on add as well, all to the same value. Subsequent modifies of a dimension affect only the
        // position1,2,4 properties. Ideally the position property should be removed altogether.
        Vector dims;
        double minPosition = 0; //
        double newPosition = minPosition;
        if (dimension.getDimType() == HspConstants.kDimTypeAttribute) {
            dims = this.getAttributeDimensions(sessionId);
            // Attribute dimensions must come AFTER other dimensions (especially the ones they're associated
            // with): Essbase constraint.
            minPosition = 100; //kMemberPositionInitial + (100 * kMemberPositionIncrement);  //101.0;
        } else {
            dims = this.getBaseDimensions(HspConstants.PLAN_TYPE_ALL, sessionId);
        }
        if (dims != null) {
            for (int i = 0; i < dims.size(); i++) {
                HspDimension dim = (HspDimension)dims.elementAt(i);
                newPosition = HspUtils.max(newPosition, dim.getPosition());
            }
            newPosition++;
            newPosition = HspUtils.max(newPosition, minPosition);
        }
        return newPosition;
    }

    private void setMemberAndSiblingPositions(HspMember member, HspActionSet actionSet, boolean generateActions, int sessionId) throws Exception {
        if (member == null)
            throw new RuntimeException("Member argument null.");

        if ((actionSet == null) && (generateActions))
            throw new RuntimeException("Action set argument cannot be null if generateActions argument is true.");

        // We will rely on the position set by the calling code and not try to recalculate the position here
        // because the following code sets the position in increasing order within a parent. What we need for
        // base time periods is for the position to be increasing for across all leaf nodes.
        if ((member.getDimId() == HspConstants.kDimensionTimePeriod) && (member instanceof HspTimePeriod) &&
            ((((HspTimePeriod)member).getType() == HspConstants.LEAF_TP_TYPE) || (((HspTimePeriod)member).getType() == HspConstants.YEAR_TP_TYPE) || (((HspTimePeriod)member).getType() == HspConstants.DTS_TP_TYPE))) {
            //  return;
        }
        HspMember parent = this.getDimMember(member.getDimId(), member.getParentId());
        HspMember parentBaseMember = getBaseMemberOrFailIfExpected(parent, actionSet);
        // make sure object type checks uses this as opposed to getObjectType - necessitated for shared members
        int objectType = parent.isSharedMember() ? parentBaseMember.getObjectType() : parent.getObjectType();

        if (isNotSortableMemberType(objectType)) // check object type
            throw new RuntimeException("Invalid Object Type for operation.");

        HspMember existingMember = this.getDimMember(member.getDimId(), member.getId());
        Map<Integer, List<HspMember>> childrenByParentMap = actionSet.getParameter(AS_PARAM_CHILDREN_MAP);
        List<HspMember> siblings = getCurrentSiblings(member, childrenByParentMap, actionSet, sessionId);
        if (member.getObjectType() == HspConstants.gObjType_AttributeMember && member instanceof HspAttributeMember) {
            updateAttributeUniqueNamePrefixIds(siblings);
        }

        if (generateActions)
            addPositionActions(member, existingMember, siblings, actionSet);
        // If this transaction is tracking the pending children per parent
        // then update the map to have the newest result after the reorder
        // is complete.
        if (childrenByParentMap != null) {
            HspObjectPositionComparator comparator = new HspObjectPositionComparator();
            HspUtils.addObjectToSortedList(siblings, member, comparator);
            childrenByParentMap.put(parent.getId(), siblings);
        }
    }

    private List<HspMember> getCurrentSiblings(HspMember member, Map<Integer, List<HspMember>> childrenByParentMap, HspActionSet actionSet, int sessionId) {
        // Look up the siblings from the pending childrenByParentMap if it
        // exists.  This is needed when multiple members are added to the same
        // parent in the same transaction as the getChildMembers methods will
        // not return members that have not been committed yet.
        int parentId = member.getParentId();
        List<HspMember> children = childrenByParentMap == null ? null : childrenByParentMap.get(parentId);
        // If a cached copy of the children are not found, then look up the
        // children from the getChildMembersWithoutMemberPTProps() method.
        // NOTE: No memberPTProps bound to members returned with this call. Get the children for the parent node specified
        if (children == null)
            children = getChildMembersWithoutMemberPTProps(member.getDimId(), parentId, sessionId);

        // Create a new list to place the siblings in so that the cached list
        // is not left in an inconsistent state if an exception occurs.
        int batchSize = 128;
        List<HspMember> siblings = new ArrayList<HspMember>(children == null ? batchSize : children.size() + batchSize);
        if (children != null) {
            // Only remove the member from the list of children if the id is > 0
            // otherwise it will remove all pending new members as well.
            if (member.getId() > 0) {
                for (HspMember child : children) {
                    if (child.getId() != member.getId())
                        siblings.add(child);
                }
            } else
                siblings.addAll(children);
        }
        return siblings;
    }

    private int addPositionActions(HspMember member, HspMember existingMember, List<HspMember> siblings, HspActionSet actionSet) {
        boolean inBatch = actionSet.getParameter(AS_PARAM_CHILDREN_MAP) != null;
        HspObjectPositioner poser = new HspObjectPositioner();
        // if existing member is null we're adding
        List<TriVal<Double, HspMember, Integer>> positionActions = poser.setMemberAndGetSiblingPositioningActions(siblings, member, existingMember);
        int numPositionActions = positionActions.size();
        HspAction action = createAction((member).getBaseMemberObjectType()); // need to create the correct type for shareds
        if (positionActions.size() > 0) {
            if (inBatch)
                throw new RuntimeException("Position reorder action detected during batch, repeat with a smaller batch size.");
        }

        for (TriVal<Double, HspMember, Integer> positionAction : positionActions) {
            HspMember obj = positionAction.getVal2();
            HspMember actionSetObject = (HspMember)actionSet.getCachedObject(obj);
            // if the object exists but isn't in the actionSet, clone it for update
            // else if the object is in the actionSet update that version
            // else we're adding a new member or updating an existing in this context, no need to clone or create an action
            if (obj.getId() > 0) // if the object exists
            {
                if (actionSetObject == null) {
                    if (obj.isReadOnly()) { // it's not in the action set so just clone it for update and bind PTMmberProps
                        obj = (HspMember)obj.cloneForUpdate();
                        obj.setOverrideLock(true);
                        // We need to rebind memberPTProps back onto the member because we used getChildMembersWithoutMemberPTProps to get the siblings.
                        // The getChildMembers method does bind memberPTProps to members but it has to clone the members first which is expensive.
                        Vector<HspMemberPTProps> props = mbrPTPropsCache.getObjects(mbrPTPropsMemberIdKeyDef, mbrPTPropsMemberIdKeyDef.createKeyFromId(obj.getId()));
                        if (props != null)
                            obj.setMemberPTPropsToBeSaved(props); // we can cast to a member without doing an instanceOf since only members have ptprops
                    }
                } else
                    obj = actionSetObject; // else it's in the actionSet so use that copy and assume memberPTPRops bound
            }
            obj.setPosition(positionAction.getVal1());

            if (obj.getId() > 0)
                actionSet.addAction(action, HspActionSet.UPDATE, obj);
            if (inBatch) {
                int index = positionAction.getVal3();
                if (index < 0 || index >= siblings.size())
                    throw new IllegalStateException("Invalid index [" + index + "] detected in sibling reorder during batch; retry with a smaller batch size.");
                HspMember originalObject = siblings.set(index, obj);
                if (originalObject.getId() != obj.getId())
                    throw new IllegalStateException("Object at index [" + index + "] in sibling reorder does not match the updated object; retry with a smaller batch size.");
            }
        }
        return numPositionActions;
    }

    private void updateAttributeUniqueNamePrefixIds(List<HspMember> members) {
        for (ListIterator<HspMember> it = members.listIterator(); it.hasNext(); ) {
            HspMember member = it.next();
            if (member.getObjectType() == HspConstants.gObjType_AttributeMember && member instanceof HspAttributeMember) {
                HspAttributeMember mbr = (HspAttributeMember)member.cloneForUpdate();
                // we need to reset the prefix property since it is not persisted.
                setAttributeUniqueNamePrefixId(mbr);
                it.set(mbr);
            }
        }
    }


    public int resequencePositionValues(Vector v, HspActionSet actionSet, boolean generateActions, boolean forward, int dummyPositionValue) throws Exception {
        return resequencePositionValues(v, actionSet, generateActions, forward, dummyPositionValue, false);
    }

    /**
     *
     * @param v
     * @param actionSet
     * @param generateActions
     * @param forward
     * @param dummyPositionValue
     * @param positionOnlyFistMember The value is true, if only one member is added to the already sorted member list, with the member having position as HspConstants.kMemberPosition1stSibling
     * @return
     * @throws Exception
     */
    public int resequencePositionValues(Vector v, HspActionSet actionSet, boolean generateActions, boolean forward, int dummyPositionValue, boolean positionOnlyFistMember) throws Exception {
        // Returns the number of changes needed to reorder the vector in a given direction. If the changes result in an underflow or overflow
        // condition this value will be negative, and this sort direction should not be used.
        // It's assumed that every element's position property in v had Math.ceil applied to it
        if (v == null)
            throw new RuntimeException("Vector argument null.");
        if ((actionSet == null) && (generateActions))
            throw new RuntimeException("Action set argument cannot be null if generateActions argument is true.");


        //debug//for (int i = 0; i < v.size(); i++)
        //debug//		 System.out.println(i+": "+((HspObject)v.elementAt(i)).getName()+" "+((HspObject)v.elementAt(i)).getPosition());

        int changesNeeded = 0; // tally number of changes to elements needed to sequence elements in this direction
        double newPosition = 0.0; // computed position value
        int direction = 0; // direction of reorder ascending or descending

        // Set up some loop variables whose values are direction dependent
        int cur;
        int increment;
        if (forward) {
            cur = 0;
            increment = 1;
            direction = ASCENDING;
        } else {
            cur = v.size() - 1;
            increment = -1;
            direction = DESCENDING;
        }
        int next = cur + increment;
        double lastPosition = dummyPositionValue - direction;
        HspObject current = (HspObject)v.elementAt(cur);
        HspObject nextO = null;
        double currentPosition = current.getPosition();
        HspObject o = null;
        double nextPosition = -1;
        HspObject firstObject = (HspObject)v.firstElement();
        HspAction action = null;
        if (firstObject != null) {
            action = createAction(((HspMember)firstObject).getBaseMemberObjectType()); // need to create the correct type for shareds
        }
        for (int i = 0; i < v.size(); i++) // limit is n-1 because we compute next in loop
        {
            // If we're at the last element no 'next' element exists, so cook up a position value for the 'next'
            if (i == v.size() - 1)
                nextPosition = -1;
            else {
                nextO = (HspObject)v.elementAt(next);
                nextPosition = nextO.getPosition();
            }
            //debug//System.out.println("comparing "+current.getName()+" to "+nextO.getName());

            newPosition = currentPosition; // incase we don't need to compute a new value
            if (!isInSequence(lastPosition, currentPosition, direction)) {
                changesNeeded++; // tally changes, one is needed for this element
                // Handle special cases at beginning. don't keep halving room on ends
                if (i == 0) {
                    if (currentPosition < 0) {
                        if (nextPosition < 0)
                            newPosition = kMemberPositionInitial;
                        else if (Math.abs(dummyPositionValue - nextPosition) > kMemberPositionIncrement)
                            newPosition = nextPosition - (kMemberPositionIncrement * direction);
                        else
                            newPosition = figurePosition(lastPosition, nextPosition, direction);
                    }
                } else // we're between two elements, so try to position it evenly between the two, if there's room
                    newPosition = figurePosition(lastPosition, nextPosition, direction);
                if (generateActions) {
                    //debug//System.out.println(((HspObject)v.elementAt(cur)).getName() + " "+((HspObject)v.elementAt(cur)).getPosition()+" -> "+newPosition+" resequence loop");
                    if (newPosition != currentPosition) {
                        if (current.getId() != -1) // an update, object exists in cache, so clone it
                        {
                            //debug//System.out.println("action generated");
                            HspObject o2 = (HspObject)actionSet.getCachedObject(current);
                            if (o2 == null)
                                o = (HspObject)current.cloneForUpdate();
                            else
                                o = o2;
                            o.setPosition(newPosition);
                            //System.out.println(" updating member for resequencing : " + o.getObjectName());
                            actionSet.addAction(action, HspActionSet.UPDATE, o);
                        } else
                            current.setPosition(newPosition); // this is an add, nothing exists in cache yet so don't clone it
                    }

                }
            }

            if (positionOnlyFistMember) {
                changesNeeded = (direction == ASCENDING) ? changesNeeded : v.size();
                break;
            }

            lastPosition = newPosition;
            currentPosition = nextPosition;
            current = nextO; //
            cur = next;
            next = next + increment;
        }

        // If the changes result in an underflow (pos < 0) or overflow (pos > max) return changesNeeded as a NEGATIVE quantity
        if (((direction == ASCENDING) && (newPosition > kMemberPositionMaximum)) || ((direction == DESCENDING) && (newPosition < 0.0)))
            return -v.size();
        else
            return changesNeeded;
    }

    private boolean isInSequence(double lastPosition, double currentPosition, int direction) {
        if (currentPosition < 0)
            return false;
        if (direction == ASCENDING)
            return (lastPosition < currentPosition ? true : false);
        else
            return (lastPosition > currentPosition ? true : false);
    }

    private double figurePosition(double lastPosition, double nextPosition, int direction) {
        // If next position is undefined (e.g. first, last constants) set it to lastPosition and increment
        if ((nextPosition < 0) || (!isInSequence(lastPosition, nextPosition, direction)))
            return lastPosition + (kMemberPositionIncrement * direction);

        // Compute a value between LastPosition and nextPosition
        double newPosition = Math.floor((lastPosition + nextPosition) / 2.0);
        // If there's no room, compute a value off the lastPosition
        if (Math.abs(newPosition - lastPosition) < 2.0)
            newPosition = lastPosition + (kMemberPositionIncrement * direction);

        return newPosition;
    }

    public synchronized void addObjectToVectorByPosition(Vector v, HspObject o, HspActionSet actionSet) throws Exception {
        // It's also assumed that the position values of objects in the vector are in the range [0, kMemberPositionMaximum]
        // The position value of the object being added can be in the range  [0, kMemberPositionMaximum], or
        // HspConstants.kMemberPosition1stSibling or HspConstants.kMemberPositionLastSibling.
        // After the insertion there may be another object in the vector with the same position value.
        // Every position value should have had have Math.ceil() applied to it.
        if (v == null)
            throw new RuntimeException("Vector argument null.");
        if (o == null)
            throw new RuntimeException("Object argument null.");
        if (actionSet == null)
            throw new RuntimeException("Action set argument null.");


        // put it in the vector relative to it's position value
        // First position
        if (o.getPosition() == HspConstants.kMemberPosition1stSibling)
            v.insertElementAt(o, 0); // stick it in the 1st position
        // Last position
        else if (o.getPosition() == HspConstants.kMemberPositionLastSibling)
            v.addElement(o); // stick it in the last position
        // somewhere relative to it's value
        else if (o.getPosition() >= 0) {
            boolean isObjectInserted = false;
            for (int i = 0; i < v.size(); i++) {
                if (o.position <= ((HspObject)v.elementAt(i)).getPosition() || (((HspObject)v.elementAt(i)).getPosition() == HspConstants.kMemberPositionLastSibling)) {
                    // No need to optimize intermediate position values here as there may be more inserts coming
                    // so it's best to defer for now.
                    v.insertElementAt(o, i);
                    isObjectInserted = true;
                    /*debug*/ //System.out.println(o.getName() + " "+o.getPosition()+" add no adjust, NO action");
                    break;
                }
            }
            if (!isObjectInserted)
                v.addElement(o);
        } // Invalid position specified: not 1st, last, or in-between
        else {
            throw new RuntimeException("Invalid position value.");


        }
    }

    public void resequenceTotalReorder(Vector v, HspActionSet actionSet) throws Exception {
        if (v == null)
            throw new RuntimeException("Vector argument null.");
        if (actionSet == null)
            throw new RuntimeException("Action set argument cannot be null if generateActions argument is true.");


        HspObject firstObject = (HspObject)v.firstElement();
        HspAction action = null;
        if (firstObject != null)
            action = createAction(((HspMember)firstObject).getBaseMemberObjectType());
        HspObject o = null;

        int j = 0;
        for (int i = kMemberPositionInitial; i <= kMemberPositionInitial + ((v.size() - 1) * kMemberPositionIncrement); i = i + kMemberPositionIncrement) {
            o = (HspObject)v.elementAt(j);
            if (i != o.getPosition()) {
                if (o.getId() != -1) // an update, so it exists in cache
                {
                    HspObject o2 = (HspObject)actionSet.getCachedObject(o);
                    /*
                    If o2 is null, then clone o if it is readonly or leave it alone if it is not.
                    If o2 is not null, them make assign make o = o2.
                    */
                    if (o2 == null) {
                        if (o.isReadOnly())
                            o = (HspObject)o.cloneForUpdate();
                    } else
                        o = o2;
                    o.setPosition(i);
                    actionSet.addAction(action, HspActionSet.UPDATE, o);
                } else
                    o.setPosition(i); // an add, we can't clone it cause it ain't in the cache yet
                /*debug*/ //System.out.println(j+": "+o.getName()+" "+o.getPosition());
            }
            j++;
        }
    }

    public void resequencePeriodsTotalReorder(double startPos, Vector<HspTimePeriod> v, HspActionSet actionSet) throws Exception {
        if (v == null)
            throw new RuntimeException("Vector argument null.");
        if (actionSet == null)
            throw new RuntimeException("Action set argument cannot be null if generateActions argument is true.");


        HspTimePeriod firstTP = v.firstElement();
        HspAction action = null;
        if (firstTP != null)
            action = createAction(firstTP.getBaseMemberObjectType());
        HspTimePeriod tp = null;

        if (startPos < kMemberPositionInitial)
            startPos = kMemberPositionInitial;
        for (int i = 0; i < v.size(); i++) {
            tp = v.elementAt(i);
            if (startPos != tp.getPosition()) {
                if (tp.getId() != -1) // an update, so it exists in cache
                {
                    HspTimePeriod tp2 = (HspTimePeriod)actionSet.getCachedObject(tp);
                    /*
                    If tp2 is null, then clone tp if it is readonly or leave it alone if it is not.
                    If tp2 is not null, them make assign make tp = tp2.
                    */
                    if (tp2 == null) {
                        if (tp.isReadOnly() && (tp.getType() == HspConstants.ALTERNATE_TP_TYPE))
                            tp = (HspTimePeriod)tp.cloneForUpdate();
                    } else
                        tp = tp2;

                    // only allow ordering of alternate time period types
                    if (tp.getType() == HspConstants.ALTERNATE_TP_TYPE) {
                        tp.setPosition(startPos++);
                        actionSet.addAction(action, HspActionSet.UPDATE, tp);
                    }
                } else {
                    //                    System.out.println("new mbr: " + tp.getMemberName() + " setting pos to: " + startPos);
                    tp.setPosition(startPos++); // an add, we can't clone it cause it ain't in the cache yet
                    /*debug*/ //System.out.println(j+": "+o.getName()+" "+o.getPosition());
                }
            } else {
                startPos++;
                //                System.out.println("Skipping member pos adj for mbr since pos already equals set pos:" + tp.getMemberName());
            }

        }
    }

    public synchronized void sortChildren(HspMember parent, boolean descending, boolean justImmediateChildren, int sessionId) throws Exception {
        // Auto-sorting on add's and update's is not implemented.
        // Eventually we may want to be able to auto-sort siblings of a node, and this would need to be done
        // whenever a member is added or updated. That member would need to be passed in to this method
        // (as well as the action set, presumably) so that the appropriate copy is updated.
        // The implementation is complicated for shared members, there appearance
        // on forms, and the HAL issue of when to actually perform the sort.
        // So for now, the sorting of children will be on-demand only.

        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));

        if (parent == null)
            throw new IllegalArgumentException("Member argument may not be null.");


        // make sure object type checks uses this as opposed to getObjectType - necessitated for shared members
        //		if (parent.isSharedMember())
        //			objectType = (getDimMember(parent.getDimId(), parent.getBaseMemberId())).getObjectType();
        //		else
        //			objectType = parent.getObjectType();
        HspMember parentBaseMember = getBaseMemberOrFailIfExpected(parent, null);
        int objectType = parent.isSharedMember() ? parentBaseMember.getObjectType() : parent.getObjectType();

        if (isNotSortableMemberType(objectType)) // check object type
            throw new RuntimeException("Invalid Object Type for operation.");


        if (!parent.hasChildren())
            return;

        Vector<HspMember> parentDescendants = new Vector<HspMember>();
        parentDescendants.addElement(parent); // always sort parent's immediate kids
        if (!justImmediateChildren) // and get all descendant parents if asked for
            getParentDescendants(parent, parentDescendants);

        //for (int i = 0; i < parentDescendants.size(); i++)
        //	System.out.println("parent: "+((HspMember)(parentDescendants.elementAt(i))).getObjectName());

        // Loop through all of the parent nodes and sort their children, putting actions in action set.
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        for (int i = 0; i < parentDescendants.size(); i++) {
            // Get the children for the parent node specified
            HspMember parent2 = parentDescendants.elementAt(i);
            if (parent2 == null)
                continue;
            Vector<HspMember> children = getChildMembers(parent2.getDimId(), parent2.getId(), sessionId);
            if ((children == null) || (children.size() < 2))
                continue;
            // sort the vector of siblings (in ascending or descending order, as specified)
            HspCSM.sortVector(children, new HspMemberNameComparator(descending));
            setPositionsOfMembersInOrderedVector(children, actionSet, sessionId);
        }

        // cross your fingers and commit the actions...
        actionSet.optimizeForBatch(false);
        actionSet.doActions();
    }

    public synchronized void setPositionsOfMembersInOrderedVector(Vector<HspMember> members, int sessionId) throws Exception {
        HspActionSet actionSet = new HspActionSet(hspSQL, hspSecDB.getUser(hspStateMgr.getUserId(sessionId)));
        setPositionsOfMembersInOrderedVector(members, actionSet, sessionId);
        actionSet.optimizeForBatch(false);
        actionSet.doActions();
    }

    public synchronized void setPositionsOfMembersInOrderedVector(Vector<HspMember> members, HspActionSet actionSet, int sessionId) throws Exception {
        // ASSUMPTION: Vector members are ordered correctly relative to each other though their position values may
        //             not be set to reflect that order. This routine does that.
        // ASSUMPTION: Vector contains members that are sortable object types, see (isNotSortableMemberType(objectType))
        // ASSUMPTION: Members in vector have valid, previously set, position values via setMemberAndSiblingPositions().
        //             E.g. the vector should NOT contain HspConstants.kMemberPosition1stSibling, etc. position values.
        if (actionSet == null)
            throw new RuntimeException("Action set argument null.");
        if (members == null)
            throw new RuntimeException("members vector argument null.");
        if (members.size() < 2)
            return; // no sorting required, vector is in order

        HspObjectPositioner poser = new HspObjectPositioner();
        List<TriVal<Double, HspMember, Integer>> actions = poser.reorder(members); //always returns a list though could be empty
        for (TriVal<Double, HspMember, Integer> action : actions) {
            HspObject obj = action.getVal2();
            if (obj.getId() <= 0)
                throw new RuntimeException("Non-existant object specified.");


            HspMember actionSetObject = (HspMember)actionSet.getCachedObject(obj);
            // if the object exists but isn't in the actionSet, clone it for update
            // else if the object is in the actionSet update that version
            if (actionSetObject == null) {
                if (obj.isReadOnly()) // it's not in the action set so just clone it for update
                    obj = (HspObject)obj.cloneForUpdate();
            } else
                obj = actionSetObject; // else it's in the actionSet so use that copy

            obj.setPosition(action.getVal1());

            HspAction updateAction = this.createAction(((HspMember)obj).getBaseMemberObjectType());
            actionSet.addAction(updateAction, HspActionSet.UPDATE, obj);
        }

    }

    private synchronized void getParentDescendants(HspMember member, Vector<HspMember> descendants) {
        if (member == null)
            throw new RuntimeException("Null member argument");


        Vector children = member.getChildren();
        if (children != null) {
            for (int i = 0; i < children.size(); i++) {
                HspMember child = (HspMember)children.elementAt(i);
                if (child == null)
                    continue;
                if (child.hasChildren()) {
                    descendants.addElement(child);
                    getParentDescendants(child, descendants);
                }
            }
        }
    }
    //TODO: SCA update this

    private void removeRedundantAttributes(HspMember member, int sessionId) {
        //TODO: SCA rewrite this for SCA's, remove next line in process.
        /*
        // attributes
        if (member.getShouldSaveAttributes())
        {
            // We will omit saving attributes on this member iff the attributes to be assigned are the same as those
            // currently assigned. Rember the attriburtesToBeSaved array is NOT a delta, but an absolute list of attributes
            // to be assigned.
            // these are the attributes to be saved
            HspAttributeMemberBinding[] attributesToSave = member.getAttributesToBeSaved();    // attribute assignments to be made.

            // Get all of the attributes that are currently assigned to this member
            Vector attributeDimensions = this.getAttributeDimensionsForBaseDim(member.getDimId());
            if ((attributeDimensions == null) || (attributeDimensions.size() == 0))
            {
                member.setAttributesToBeSaved(null);
                return;
            }

            List existingAttributes = new ArrayList();
            for (int i = 0; i < attributeDimensions.size(); i++)
            {
                int attribDimId = ((HspAttributeDimension)(attributeDimensions.elementAt(i))).getId();
                Vector attribs = hspFMDB.getAttributeMemberBindingsForAllPerspectives(attribDimId, member.getId());

            }
            // If we have an empty attributesToSave array it means that all attribute assignments are to
            // be deleted, but if there are no attributes to delete we don't need to do anything.
            if ((attributesToSave.length == 0) && (existingAttributes.size() == 0))
            {
                member.setAttributesToBeSaved(null);
                return;
            }
            // If there is a diffenece in the # of attributes to be saved and the existing attributes do the save
            // as this represents at least an addition or deletion, so return to update the attributes.
            if (existingAttributes.size() != attributesToSave.length)
            {
                //System.out.println("different # of attributes, updating attributes.");
                return;
            }

            // We now have the attributes to be saved and the existing attributs and the number of each is the same.
            // WHat we need to do now is see if any are different, if so we need to update the attributes. If all of the
            // attributes are the same we don't need to update the attributes.
            for (int i = 0; i < existingAttributes.size(); i++)
            {
                HspAttributeMemberBinding attributeToSave = attributesToSave[i];
                boolean foundExisting = false;
                for (int j = 0; j < existingAttributes.size(); j++)
                {
                    HspAttributeMember existingAttribute = (HspAttributeMember)existingAttributes.get(i);
                    if ((existingAttribute == null) || (attributeToSave == null))
                        throw  new RuntimeException("Fetched or to-save attribute was null");
        //todo: SCA  this commented out for the time being to get past compile: reimplement this!
                    //
                    //  if (HspUtils.equals(existingAttribute.getName(), attributeToSave.getName()))
                    //  {
                    //      foundExisting = true;
                    //      break;
                    //  }


                }
                // An existing attribute binding was not found for an attribute to be saved, so return to rebind the
                // attributes
                if (!foundExisting)
                {
                    //System.out.println("Attribute value changed, updating attributes.");
                    return;
                }
            }

            // If we get here all of the attributes specified to be assigned are already assigned to the member so we
            // don't need to save them.
            //System.out.println("All attributes already assigned to member, not saving any attributes.");
            member.setAttributesToBeSaved(null);
        }
        */
    }

    private void INVALIDremoveRedundantAliases(HspMember member, int sessionId) {
        // aliases
        // Replace the aliasesToSave array on the member with an array of only those aliases that NEED to
        // be saved. This is done to prevent redundant writes to the DB.
        // Loop throgh the aliases to be saved and if they are not already bound to the member add them to the
        // 'must-save' list. Finally replace the aliasesToBeSaved array on the member with the must-save list/array.
        // Also, only put deletes into must-save array iff an alias exists for the table.
        if (member.getShouldSaveAlias()) {
            Object[][] aliasesToSave = member.getAliasesToBeSaved();
            //Object[][] aliasesMustSave = new Object[HspConstants.MAXIMUM_NUMBER_OF_ALIAS_TABLES][2];
            List<Object[]> aliasesMustSave = null;
            if ((aliasesToSave != null) && (aliasesToSave.length > 0)) {
                // See if an alias for this table id exists already and then see if it changed. If so, add
                // it to the must-save array. Don't add it if it didn't change. Also, DO add it if no alias
                // exists for the table id.
                aliasesMustSave = new ArrayList<Object[]>();
                int mustSaveIndex = 0;
                for (int i = 0; i < aliasesToSave.length; i++) {
                    int aliasToSaveTableId = (Integer)(aliasesToSave[i][0]);
                    String aliasToSaveName = (String)aliasesToSave[i][1];
                    HspAlias existingAlias = hspAlsDB.getAlias(aliasToSaveTableId, member.getId());
                    String existingAliasName = null;
                    // ignore attempt to delete an alias that doesn't exist
                    if ((existingAlias == null) && (aliasToSaveName == null))
                        continue;
                    // if an alias exists..
                    if (existingAlias != null) {
                        existingAliasName = existingAlias.getName();
                        if (existingAliasName == null) // an existing alias name should never be null so this should never execute
                        {
                            aliasesMustSave.add(aliasesToSave[i]); // this saves [i][0] and [i][1]
                        } else {
                            // save the new alias only if it's name is different than the existing one or if it's
                            // name is null (which means delete it)
                            if ((aliasToSaveName == null) || (aliasToSaveName.compareTo(existingAliasName) != 0)) {
                                aliasesMustSave.add(aliasesToSave[i]); // this saves [i][0] and [i][1]
                            }
                        }
                    }
                    // else an alias for that table does not exist, so save it if it's not null (a delete)
                    else {
                        if (aliasToSaveName != null) {
                            aliasesMustSave.add(aliasesToSave[i]); // this saves [i][0] and [i][1]
                        }
                    }
                }
                // We reduced the number of aliases to save so create this new smaller array and stick it on the member
                if ((aliasesMustSave != null) && (aliasesMustSave.size() > 0) && (aliasesMustSave.size() < aliasesToSave.length)) {
                    Object[][] finalAliases = aliasesMustSave.toArray(new Object[aliasesMustSave.size()][2]);
                    //System.out.println("saving fewer than specified (optimized) aliases for "+member.getName());
                    member.setAliasesToBeSaved(finalAliases);
                    member.setShouldSaveAlias(true);
                }
                // all the aliases that were meant to be saved would have been redundant so do nothing
                else if ((aliasesMustSave != null) && (aliasesMustSave.size() == 0)) {
                    member.setAliasesToBeSaved(null);
                    member.setShouldSaveAlias(false);
                    //System.out.println("(1)Not saving aliases for "+member.getName());
                }
                // else the original aliases array had no redundancies so just let it be on the member
            }
        } else {
            member.setAliasesToBeSaved(null);
            member.setShouldSaveAlias(false);
            //System.out.println("(2)Not saving aliases for "+member.getName());
        }
    }

    private Object[][] getMinimalAliases(Object[][] aliasesToSave, int memberId, int sessionId) {
        // aliases
        // Return an aliasesToSave array with an array of only those aliases that NEED to
        // be saved. This is done to prevent redundant writes to the DB.
        // Loop throgh the aliases to be saved and if they are not already bound to the member add them to the
        // 'must-save' list. Finally replace the aliasesToBeSaved array on the member with the must-save list/array.
        // Also, only put deletes into must-save array iff an alias exists for the table.
        // If null is returned, the aliasesToBeSaved on the member should be set to null and
        // the shouldSaveAliases should be set to false.
        List<Object[]> aliasesMustSave = null;
        if ((aliasesToSave != null) && (aliasesToSave.length > 0)) {
            // See if an alias for this table id exists already and then see if it changed. If so, add
            // it to the must-save array. Don't add it if it didn't change. Also, DO add it if no alias
            // exists for the table id.
            aliasesMustSave = new ArrayList<Object[]>();
            int mustSaveIndex = 0;
            for (int i = 0; i < aliasesToSave.length; i++) {
                int aliasToSaveTableId = (Integer)(aliasesToSave[i][0]);
                String aliasToSaveName = (String)aliasesToSave[i][1];
                HspAlias existingAlias = hspAlsDB.getAlias(aliasToSaveTableId, memberId);
                String existingAliasName = null;
                // ignore attempt to delete an alias that doesn't exist
                if ((existingAlias == null) && (aliasToSaveName == null))
                    continue;
                // if an alias exists..
                if (existingAlias != null) {
                    existingAliasName = existingAlias.getName();
                    if (existingAliasName == null) // an existing alias name should never be null so this should never execute
                    {
                        aliasesMustSave.add(aliasesToSave[i]); // this saves [i][0] and [i][1]
                    } else {
                        // save the new alias only if it's name is different than the existing one or if it's
                        // name is null (which means delete it)
                        if ((aliasToSaveName == null) || (aliasToSaveName.compareTo(existingAliasName) != 0)) {
                            aliasesMustSave.add(aliasesToSave[i]); // this saves [i][0] and [i][1]
                        }
                    }
                }
                // else an alias for that table does not exist, so save it if it's not null (a delete)
                else {
                    if (aliasToSaveName != null) {
                        aliasesMustSave.add(aliasesToSave[i]); // this saves [i][0] and [i][1]
                    }
                }
            }
            // We reduced the number of aliases to save so create this new smaller array and stick it on the member
            if ((aliasesMustSave != null) && (aliasesMustSave.size() > 0) && (aliasesMustSave.size() < aliasesToSave.length)) {
                Object[][] finalAliases = aliasesMustSave.toArray(new Object[aliasesMustSave.size()][2]);
                //System.out.println("saving fewer than specified (optimized) aliases for "+member.getName());
                return finalAliases;
            }
            // all the aliases that were meant to be saved would have been redundant so do nothing
            else if ((aliasesMustSave != null) && (aliasesMustSave.size() == 0)) {
                return null;
                //System.out.println("(1)Not saving aliases for "+member.getName());
            }
            // else the original aliases array had no redundancies so just let it be on the member
        }
        // array was null or empty
        return null;
    }

    /*
     * Validates that certain member properties are enabled and can be changed.
     * Validation is based on application type mainly FCCS.
     */

    private void validateFeatureIsEnabled(HspMember member) {
        HspUtils.verifyArgumentNotNull(member, "Member");

        if (!hspSystemConfig.isFCCSApp() && !hspSystemConfig.isTrcsApp()) {
            int dimType = 0;

            if (member.getObjectType() == HspConstants.gObjType_Metric || member.getDimId() == HspConstants.kDimensionMetric)
                dimType = HspConstants.kDimTypeMetric;
            else if (member.getObjectType() == HspConstants.gObjType_ReplacementMember || member.getObjectType() == HspConstants.gObjType_ReplacementDimension)
                dimType = HspConstants.kDimTypeReplacementDimension;

            String feature = "";
            boolean unsupportedFeature = false;
            if (dimType == HspConstants.kDimTypeMetric) {
                feature = "MSG_UNSUPPORTED_FEATURE_METRIC";
                unsupportedFeature = true;
            } else if (dimType == HspConstants.kDimTypeReplacementDimension) {
                feature = "MSG_UNSUPPORTED_FEATURE_REPLACEMENT";
                unsupportedFeature = true;
            } else if (member.isUpperLevelEntityInput()) {
                feature = "MSG_UNSUPPORTED_FEATURE_UPPER_ENTITY_INPUT";
                unsupportedFeature = true;
            }

            if (unsupportedFeature) {
                Properties p = new Properties();
                p.put("MEMBER_NAME", member.getMemberName());
                throw new HspRuntimeException(feature, p);
            }
        }
    }

    private void validateAttributeDimensionForAdd(HspAttributeDimension dimension) {
        if (dimension == null)
            throw new RuntimeException("Invalid dimension: null");
        if (dimension.getObjectType() != HspConstants.gObjType_AttributeMember)
            throw new RuntimeException("Invalid object type for Attribute Dimension.");
        if (dimension.getParentId() != HspConstants.gFolder_AttrDims)
            throw new RuntimeException("Invalid parent id for Attribute Dimension.");


        // disable for 9.3.1
        //if (dimension.getTwopassCalc())
        //	throw  new RuntimeException("Invalid Two Pass Calculation setting");
        for (int plan = 1; plan <= HspConstants.PLAN_TYPE_ALL; plan = plan << 1) {
            if (dimension.getDensity(plan) != HspConstants.kDataDensitySparse)
                throw new RuntimeException("Invalid Density setting for plan " + plan + " on dimension " + dimension.getName() + ", must be sparse.");


        }

        validateAttributeType(dimension.getAttributeType());
        if (dimension.isDimEditor())
            throw new RuntimeException("Invalid setting of Attribute Dimension as Dimension Editor dimension: " + dimension.isDimEditor());


        //TODO: when deletes of dimensions are supported positions on cubes will need to be doubles
        int position = (int)(dimension.getPosition());
        dimension.setPosition(HspConstants.PLAN_TYPE_ALL, position);

        //dimenson.setPositon(PLAN_TYPE_ALL, 10);
        //dimesnion.setPosiiotn(usedIn, 10);
        // Make sure that if we're adding an Slowly Changing attribute dimension that it's usedIn is
        // set for workforce only. And if we're adding a non-sca attribute dimension that it's usedIn is set to
        // the usedIn of the associated base dimension.
        HspDimension baseDimension = getDimRoot(dimension.getBaseDimId());
        // validate base dimension property
        if ((baseDimension == null) || (baseDimension.getDimType() == HspConstants.kDimTypeAttribute))
            throw new RuntimeException("Invalid base dimension specified for \"" + dimension.getObjectName() + "\" attribute dimension.");


        // make sure the attribute dimension's usedIn is a subset of the base dimension's usedIn
        if ((baseDimension.getUsedIn() & dimension.getUsedIn()) != dimension.getUsedIn())
            throw new RuntimeException("The non-sca attribute dimension \"" + dimension.getObjectName() + "\" usedIn (" + dimension.getUsedIn() + ") must be a subset of the base dimension's usedIn (" + baseDimension.getUsedIn() + ").");
    }


    private void validateAttributeDimensionForUpdate(HspAttributeDimension oldDimension, HspAttributeDimension dimension) {
        if (dimension == null)
            throw new RuntimeException("Invalid dimension: null");
        if (oldDimension == null)
            throw new RuntimeException("Invalid dimension: null");
        if (oldDimension.getUsedIn() != dimension.getUsedIn())
            throw new RuntimeException("Attribute dimension usedIn cannot be changed directly, only indirectly by change on base dimension's usedIn.");


        validateAttributeDimensionForAdd(dimension);
        //		Commented out because if the AttributeDimension doesnot have any children then the AttributeType should be allowed to change
        //		if (oldDimension.getAttributeType() != dimension.getAttributeType())
        //			throw  new RuntimeException("Attribute type cannot be modified for Attribute Dimension.");
        if (oldDimension.getBaseDimId() != dimension.getBaseDimId())
            throw new RuntimeException("Base dimension Id cannot be modified for Attribute Dimension.");


    }

    private void validateUserDefinedDimensionForAdd(HspDimension dimension, int sessionId) {
        if (dimension == null)
            throw new RuntimeException("Invalid dimension: null");
        if (dimension.getObjectType() != HspConstants.gObjType_UserDefinedMember)
            throw new RuntimeException("Invalid object type for Dimension.");
        if (dimension.getParentId() != HspConstants.gFolder_Dimensions)
            throw new RuntimeException("Invalid Parent Id for Dimension");
        if (!dimension.isDimEditor())
            throw new RuntimeException("Invalid setting of Dimension Editor dimension property: " + dimension.isDimEditor());


        // took out for 9.3.1
        //if (dimension.getTwopassCalc())
        //	throw  new RuntimeException("Invalid Two Pass Calculation setting");
        for (int plan = 1; plan <= HspConstants.PLAN_TYPE_ALL; plan = plan << 1) {
            if (dimension.getDensity(plan) != HspConstants.kDataDensitySparse && dimension.getDensity(plan) != HspConstants.kDataDensityDense)
                throw new RuntimeException("Invalid Density " + plan + " setting on dimension " + dimension.getName());


        }
        //TODO: when deletes of dimensions are supported positions on cubes will need to be doubles
        //		int position = (int)(dimension.getPosition());
        //    dimension.setPosition(HspConstants.PLAN_TYPE_ALL,  position);
        setCubePositionsOnDimensionToMaxExistingPlusOne(dimension, sessionId);
    }
    //todo: this method needs to be rewritten when support for multiple cubes in implemented

    private void setCubePositionsOnDimensionToMaxExistingPlusOne(HspDimension dimension, int sessionId) {
        final int PLAN_TYPE_1 = 1;
        final int PLAN_TYPE_2 = 2;
        final int PLAN_TYPE_3 = 4;
        final int PLAN_TYPE_4 = 8;
        final int PLAN_TYPE_5 = 16;
        final int PLAN_TYPE_6 = 32;
        final int PLAN_TYPE_7 = 64;
        dimension.setPosition(HspConstants.PLAN_TYPE_ALL, 0); // init all cube positions on dim to absolute min value

        // loop over all dimensions and set the cube positions on the dimension passed in to the max of what was seen
        // for all dimensions
        Vector<HspDimension> dims = this.getBaseDimensions(HspConstants.PLAN_TYPE_ALL, sessionId);
        if (dims.size() > 1) {
            for (HspDimension dim : dims) {
                for (int i = 1; i <= HspConstants.PLAN_TYPE_ALL; i = i << 1) {
                    if ((i & HspConstants.PLAN_TYPE_ALL) == 0)
                        continue;
                    switch (i) {
                    case PLAN_TYPE_1:
                        dimension.setPosition1(Math.max(dim.getPosition(PLAN_TYPE_1), dimension.getPosition(PLAN_TYPE_1)));
                        break;
                    case PLAN_TYPE_2:
                        dimension.setPosition2(Math.max(dim.getPosition(PLAN_TYPE_2), dimension.getPosition(PLAN_TYPE_2)));
                        break;
                    case PLAN_TYPE_3:
                        dimension.setPosition3(Math.max(dim.getPosition(PLAN_TYPE_3), dimension.getPosition(PLAN_TYPE_3)));
                        break;
                    case PLAN_TYPE_4:
                        dimension.setPosition4(Math.max(dim.getPosition(PLAN_TYPE_4), dimension.getPosition(PLAN_TYPE_4)));
                        break;
                    case PLAN_TYPE_5:
                        dimension.setPosition5(Math.max(dim.getPosition(PLAN_TYPE_5), dimension.getPosition(PLAN_TYPE_5)));
                        break;
                    case PLAN_TYPE_6:
                        dimension.setPosition6(Math.max(dim.getPosition(PLAN_TYPE_6), dimension.getPosition(PLAN_TYPE_6)));
                        break;
                    case PLAN_TYPE_7:
                        dimension.setPosition7(Math.max(dim.getPosition(PLAN_TYPE_7), dimension.getPosition(PLAN_TYPE_7)));
                        break;
                    }
                }
            }
            dimension.setPosition1(dimension.getPosition(PLAN_TYPE_1) + 1);
            dimension.setPosition2(dimension.getPosition(PLAN_TYPE_2) + 1);
            dimension.setPosition3(dimension.getPosition(PLAN_TYPE_3) + 1);
            dimension.setPosition4(dimension.getPosition(PLAN_TYPE_4) + 1);
            dimension.setPosition5(dimension.getPosition(PLAN_TYPE_5) + 1);
            dimension.setPosition6(dimension.getPosition(PLAN_TYPE_6) + 1);
            dimension.setPosition7(dimension.getPosition(PLAN_TYPE_7) + 1);
        }
    }

    private void validateUserDefinedDimensionForUpdate(HspDimension dimension) {
        if (dimension == null)
            throw new RuntimeException("Invalid dimension: null");


        switch (dimension.getObjectType()) {
        case HspConstants.gObjType_Scenario:
            break;
        case HspConstants.gObjType_UserDefinedMember:
            //User defined members must have a property of DimEditor Except for HSP_RATES dimension
            if (!dimension.isDimEditor() && (dimension.getId() != HspConstants.kDimensionRates) && (dimension.getId() != HspConstants.kDimensionView))
                throw new RuntimeException("Invalid setting of Dimension Editor dimension property: " + dimension.isDimEditor());
            if (dimension.getParentId() != HspConstants.gFolder_Dimensions)
                throw new RuntimeException("Invalid Parent Id for Dimension");
            break;
        case HspConstants.gObjType_Version:
            break;
        case HspConstants.gObjType_Dimension:
            break;
        }
        for (int plan = 1; plan <= HspConstants.PLAN_TYPE_ALL; plan = plan << 1) {
            if (dimension.getDensity(plan) != HspConstants.kDataDensitySparse && dimension.getDensity(plan) != HspConstants.kDataDensityDense)
                throw new RuntimeException("Invalid Density " + plan + " setting on dimension " + dimension.getName());
        }
    }


    ////////////////////////////////////////////////////////////
    // END OF New Methods for DEDB which make HAL work
    ////////////////////////////////////////////////////////////


    ////////////////////////////////////////////////////////////
    // BEGIN Public static utility methods
    ////////////////////////////////////////////////////////////


    public void validateConsolidationOperator(int consolidationOperator, int planType) throws RuntimeException {
        if (notInRange(consolidationOperator, HspConstants.kDataConsolRangeLow, HspConstants.kDataConsolRangeHigh)) {
            HspCube cube = getCubeByPlanType(planType);
            throw new RuntimeException("Invalid Consolidation Operator specified for plan " + cube.getPlanTypeName());

        }
    }

    private void checkHierarchyTypeForASO(HspMember member, int planType) {
        if (member.getHierarchyType() == HspConstants.ESS_MULTIPLE_HIERARCHY_NOT_SET) {
            HspCube cube = getCubeByPlanType(planType);

            if (cube != null && cube.getType() == HspConstants.ASO_CUBE) {
                member.setHierarchyType(HspConstants.ESS_STORED_HIERARCHY);
            }
        }
    }

    private void validateConsolidationOperatorForASO(HspMember member, int planType) throws RuntimeException {
        HspCube cube = getCubeByPlanType(planType);
        if ((cube != null) && (member != null) && (cube.getType() == HspConstants.ASO_CUBE) && ((member.getUsedIn() & planType) == planType)) {
            //21932647 - skipping validation for setting Never consol op in case of Account dimension with specified data types
            int dataType = member.getDataType();
            boolean allowNeverConsolOp = member.getDimId() == HspConstants.kDimensionAccount && (dataType == HspConstants.DATA_TYPE_TEXT || dataType == HspConstants.DATA_TYPE_DATE || dataType == HspConstants.DATA_TYPE_ENUMERATION);
            if (member.getConsolOp(planType) == HspConstants.kDataConsolNever && !allowNeverConsolOp) {
                Properties p = new Properties();
                p.put("MEMBER_NAME", member.getMemberName());
                throw new HspRuntimeException("MSG_CL_ERROR__INVALID_NEVER_OP_FOR_ASO", p);
            }
            int hierType = getHierarchyTypeForMember(cube, member);
            if ((hierType == HspConstants.ESS_STORED_HIERARCHY || hierType == HspConstants.ESS_MULTIPLE_HIERARCHY_NOT_SET) && (member.getDimId() != HspConstants.kDimensionAccount)) {
                if (member.getConsolOp(planType) != HspConstants.kDataConsolAddition && member.getConsolOp(planType) != HspConstants.kDataConsolIgnore) {
                    Properties p = new Properties();
                    p.put("MEMBER_NAME", member.getMemberName());
                    throw new HspRuntimeException("MSG_CL_ERROR__INVALID_CONSOL_OP_FOR_ASO", p);
                }
            }
        }
    }
    /*
	* No longer necessary, since arg type was changed from int to boolean which, by definition,
	* will be set to a valid value
	public void validateUsedForConsolidation (int usedForConsolidation) throws RuntimeException
	{
		if (!(usedForConsolidation == HspConstants.kDataYes) || (usedForConsolidation == HspConstants.kDataNo))
			throw new RuntimeException("Invalid UsedForConsolidation value.");
	}
	*/

    public static void validateObjectDescription(String description) throws RuntimeException {
        // string may be null, which is okay
        // No trimming done: responsibility of the callers
        if (description != null)
            if (description.length() > ObjectDescriptionMaximumLength)
                throw new RuntimeException("Object description exceeds maximum length.");


    }

    public void validateAttributeAssignmentAgainstDataStorage(HspMember member) throws RuntimeException {
        // Attributes cannot be associated with Shared Members only their base members, nor can attributes be
        // associated with label-only members. For label-only members let attributes be deleted (a zero-length
        // attributesToBeSaved array. This supports the case of changing an existing member from non label-only to
        // label-only, deleting attribute assignments in the process.
        if ((member.isSharedMember() || ((member.getDataStorage() == HspConstants.kDataStorageLabelOnly) && member.getAttributesToBeSaved() != null && member.getAttributesToBeSaved().length != 0)) && member.getShouldSaveAttributes() &&
            containsIndexedAttributes(member.getAttributesToBeSaved()))
            throw new HspRuntimeException("MSG_NO_ATTRIBUTES_ON_SHARED_OR_LABEL_ONLY");


    }

    public void validateAliases(HspMember member) throws RuntimeException {
        // validate the aliases
        Object[][] aliases = member.getAliasesToBeSaved();
        validateAliases(aliases);
    }

    public void validateAliases(Object[][] aliases) throws RuntimeException {
        // validate the aliases
        if ((aliases != null) && (aliases.length > 0)) {
            for (int i = 0; i < aliases.length; i++) {
                // Make sure the object id specified for the table is ok
                HspObject table = hspAlsDB.getAliasTable((Integer)aliases[i][0]);
                if (table == null)
                    throw new RuntimeException("Cannot add alias: alias table id is undefined.");


                // validate the alias name, null means delete existing alias so pass it on through
                if (aliases[i][1] != null && !aliases[i][1].toString().equals(""))
                    validateDimensionMemberName(aliases[i][1].toString());
            }
        }
    }

    private void validateDataType(HspMember member) throws RuntimeException {
        if ((member.getDataType() != HspConstants.DATA_TYPE_UNSPECIFIED) && (member.getDataType() != HspConstants.DATA_TYPE_CURRENCY) && (member.getDataType() != HspConstants.DATA_TYPE_NONCURRENCY) && (member.getDataType() != HspConstants.DATA_TYPE_PERCENTAGE) &&
            (member.getDataType() != HspConstants.DATA_TYPE_ENUMERATION) && (member.getDataType() != HspConstants.DATA_TYPE_DATE) && (member.getDataType() != HspConstants.DATA_TYPE_TEXT))
            throw new RuntimeException("Invalid Data Type value specified for member \"" + member.getName() + "\"");


    }

    public void validateDataStorage(HspMember member) throws RuntimeException {
        HspMemberPTProps[] mbrPTProps = member.getMemberPTPropsToBeSaved();
        int dataStorage = member.getDataStorage();
        int priorDataStorage = member.getDataStorage();
        int dataStoragePT0 = member.getDataStorage();
        int dataStoragePT1 = member.getDataStorage();
        int dataStoragePTN = 0;
        int planType = 0;
        int totalPT = 0;
        boolean labelOnlySeen = false;
        boolean sharedSeen = false;
        boolean nonSharedNonLabelSeen = false;
        boolean priorSet = false;
        boolean allSame = true;
        int nTimes = 0;
        if (mbrPTProps != null)
            nTimes = mbrPTProps.length;

        /*
            // check if all plan type specific values are the same
   /*         for (int i = 0; i < nTimes; i++)
            {
                HspMemberPTProps mbrPTProp = mbrPTProps[i];
                planType = mbrPTProp.getPlanType();

                if (planType == 0)
                  dataStoragePT0 = mbrPTProp.getDataStorage();
                else if (planType == 1)
                  dataStoragePT1 = mbrPTProp.getDataStorage();
                if ((member.getUsedIn() & planType) == 0)
                    continue;  // don't validate data storage if the plan type isn't 'used in'
                totalPT += planType;
                dataStorage = mbrPTProp.getDataStorage();
                if (dataStorage == HspConstants.kDataStorageSharedMember)
                  sharedSeen = true;
                else if (dataStorage == HspConstants.kDataStorageLabelOnly)
                  labelOnlySeen = true;
                else
                  dataStoragePTN = dataStorage;
                if (i > 0 && priorSet && priorDataStorage != dataStorage)
                    allSame = false;

                priorDataStorage = dataStorage;
                priorSet = true;
            }

            // reset default data storeage to the same value if all PT values are the same
            if (allSame)
	              member.setDataStorage(dataStorage);
            else {
                if (nTimes > 0 && totalPT == member.getUsedIn()) {
                    if (member.getDataStorage() == HspConstants.kDataStorageSharedMember  && !sharedSeen) {
                       if(dataStoragePT0 != HspConstants.kDataStorageSharedMember)
                           member.setDataStorage(dataStoragePT0);
                       else if(dataStoragePT1 != HspConstants.kDataStorageSharedMember)
                           member.setDataStorage(dataStoragePT1);
                       else
                           member.setDataStorage(dataStoragePTN);
                    }
                    else if (member.getDataStorage() == HspConstants.kDataStorageLabelOnly  && !labelOnlySeen) {
                       if(dataStoragePT0 != HspConstants.kDataStorageLabelOnly)
                           member.setDataStorage(dataStoragePT0);
                       else if(dataStoragePT1 != HspConstants.kDataStorageLabelOnly)
                           member.setDataStorage(dataStoragePT1);
                       else
                           member.setDataStorage(dataStoragePTN);
                    }
                }
            }
*/
        // reinitialize for normal validation check
        boolean hasFormula = false;
        String mbrFormula = member.getFormula();
        sharedSeen = false;
        labelOnlySeen = false;
        dataStorage = member.getDataStorage();
        planType = 0;
        if (mbrFormula != null && mbrFormula.length() > 0)
            hasFormula = true;

        for (int i = -1; i < nTimes; i++) {
            if (i > -1) {
                HspMemberPTProps mbrPTProp = mbrPTProps[i];
                dataStorage = mbrPTProp.getDataStorage();
                mbrFormula = mbrPTProp.getFormula();
                planType = mbrPTProp.getPlanType();
                if ((member.getUsedIn() & planType) == 0)
                    continue; // don't validate data storage if the plan type isn't 'used in'
            }


            if (mbrFormula != null && mbrFormula.length() > 0)
                hasFormula = true;

            if (dataStorage == HspConstants.kDataStorageLabelOnly)
                labelOnlySeen = true;
            else if (dataStorage == HspConstants.kDataStorageSharedMember)
                sharedSeen = true;
            else
                nonSharedNonLabelSeen = true;

            if (notInRange(dataStorage, HspConstants.kDataStorageRangeLow, HspConstants.kDataStorageRangeHigh)) {
                Properties p = new Properties();
                p.put("DIMENSION_MEMBER_NAME", member.getMemberName());
                throw new HspRuntimeException("MSG_INVALID_DATA_STORAGE_VALUE", p, new RuntimeException("Invalid Data Storage Property value for member: " + member.getMemberName()));


            }
            if (dataStorage == HspConstants.kDataStorageSharedMember && !member.isSharedMember()) {
                Properties p = new Properties();
                p.put("DIMENSION_MEMBER_NAME", member.getMemberName());
                throw new HspRuntimeException("MSG_DTASTOR_SHARED_ON_NON_SHARED_MBR", p, new RuntimeException("Cannot set Shared Data Storage on non-shared member: " + member.getMemberName()));
            }
            if (hasFormula && (dataStorage == HspConstants.kDataStorageSharedMember || dataStorage == HspConstants.kDataStorageLabelOnly)) {
                Properties p = new Properties();
                p.put("DIMENSION_MEMBER_NAME", member.getMemberName());
                throw new HspRuntimeException("MSG_DTASTOR_SHARED_WITH_FORMULA", p, new RuntimeException("Cannot set Shared or Label Only Data Storage with a formula on member: " + member.getMemberName()));
            }

            if (member.isSharedMember() && dataStorage != HspConstants.kDataStorageSharedMember) {
                Properties p = new Properties();
                p.put("DIMENSION_MEMBER_NAME", member.getMemberName());
                throw new HspRuntimeException("MSG_SHARED_MBR_MUST_BE_SHARED_FOR_ALL_PTS", p, new RuntimeException("Shared members must have Shared Data Storage for all plan types, member: " + member.getMemberName()));
            }

            if (member.hasLabelOnlyDataStorage() && dataStorage != HspConstants.kDataStorageLabelOnly) {
                Properties p = new Properties();
                p.put("DIMENSION_MEMBER_NAME", member.getMemberName());
                throw new HspRuntimeException("MSG_LABELONLY_MBR_MUST_BE_LABELONLY_FOR_ALL_PTS", p, new RuntimeException("Label Only members must have Label Only Data Storage for all plan types, member: " + member.getMemberName()));
            }
            // Can't have Label Only Data Storage on a level 0 (leaf) node
            /* how else you gonna add one? Let it error out on cube refresh
		if ((member.getDataStorage() == HspConstants.kDataStorageLabelOnly) && ((member.getId() == -1) || (!member.hasChildren())) && (!member.isRootMember()))
		{
				Properties p = new Properties();
				p.put("DIMENSION_MEMBER_NAME", member.getMemberName());
				throw new HspRuntimeException("MSG_NO_LABEL_ONLY_LEVEL_0",p, new RuntimeException("Level 0 members cannot have Label Only Data Storage, member: "+member.getMemberName()));
		}
		*/
            if ((dataStorage == HspConstants.kDataStorageSharedMember) && (!supportsSharedMembers(member.getDimId()))) {
                HspDimension dim = getDimRoot(member.getDimId());
                Properties p = new Properties();
                p.put("DIMENSION_MEMBER_NAME", member.getMemberName());
                p.put("DIMENSION_NAME", dim.getMemberName());
                throw new HspRuntimeException("MSG_SHARED_MEMBERS_NOT_SUPPORTED_THIS_DIMENSION", p, new RuntimeException("Shared Members not supported for Dimension: " + dim.getName() + " member: " + member.getMemberName()));


            }

            if (member instanceof HspDimension) {
                // LabelOnly, dynamic calc, and dynamic calc and store data storage is INvalid for Entity, Version,
                // Currency, and UserDefined Dimensions for multi-currency app's
                if ((hspSystemConfig.isMultiCurrency()) && ((dataStorage == HspConstants.kDataStorageLabelOnly) || (dataStorage == HspConstants.kDataStorageDynamic) || (dataStorage == HspConstants.kDataStorageDynCalcStore))) {

                    switch ((member).getObjectType()) // << on dimension, set to HspConstants.kDimTypeUser
                    {
                    case HspConstants.kDimensionEntity:
                    case HspConstants.kDimensionVersion:
                    case HspConstants.gObjType_Currency:
                    case HspConstants.gObjType_UserDefinedMember:
                        throw new HspRuntimeException("MSG_NO_LABEL_ONLY_THIS_DIMENSION_APP",
                                                      new RuntimeException("Label Only, Dynamic Calc, or Dynamic Calc and Store Data Storage cannot be set for Entities, Versions, Currencies, and UserDefined dimensions in multi-currency applications for Dimension: " +
                                                                           member.getMemberName()));

                        //break;
                    }
                }
                if (dataStorage == HspConstants.kDataStorageSharedMember) {
                    Properties p = new Properties();
                    p.put("DIMENSION_NAME", member.getMemberName());
                    throw new HspRuntimeException("MSG_DATA_STORAGE_SHARED_ON_DIMENSION", p, new RuntimeException("Invalid Data Storage value (Shared) for Dimension: " + member.getName()));
                }
            }
        } // for

        // if labelOnly or shared is specified for a plan type it must be specified for all plan types
        if (labelOnlySeen && (sharedSeen || nonSharedNonLabelSeen)) {
            Properties p = new Properties();
            p.put("DIMENSION_MEMBER_NAME", member.getMemberName());
            throw new HspRuntimeException("MSG_LABELONLY_MBR_MUST_BE_LABELONLY_FOR_ALL_PTS", p, new RuntimeException("Label Only members must have Label Only Data Storage for all plan types, member: " + member.getMemberName()));
        }

        if (sharedSeen && (labelOnlySeen || nonSharedNonLabelSeen)) {
            Properties p = new Properties();
            p.put("DIMENSION_MEMBER_NAME", member.getMemberName());
            throw new HspRuntimeException("MSG_SHARED_MBR_MUST_BE_SHARED_FOR_ALL_PTS", p, new RuntimeException("Shared members must have Shared Data Storage for all plan types, member: " + member.getMemberName()));
        }
    }
    /*
	* No longer necessary, since arg type was changed from int to boolean which, by definition,
	* will be set to a valid value
	public void validateTwoPassCalculation (int twoPassCalculation) throws RuntimeException
	{
		if (!(twoPassCalculation == HspConstants.kDataTwoPassCalcYes) || (twoPassCalculation == HspConstants.kDataTwoPassCalcNo))
			throw new RuntimeException("Invalid Two Pass Calculation value.");
	}
	*/

    public void validatePlanTypesAndSourcePlanType(HspMember member) throws RuntimeException {
        validatePlanTypesAndSourcePlanType(member, null);
    }

    public void validatePlanTypesAndSourcePlanType(HspMember member, HspActionSet actionSet) throws RuntimeException {
        // Make certain Source Plan Type is included in Selected Plan Types and that Selected Plan Types
        // is a subset of the Parent's Selected Plan Types.
        int parentUsedIn = HspConstants.PLAN_TYPE_NONE;
        int memberUsedIn = HspConstants.PLAN_TYPE_NONE;
        int memberSourcePlanType = HspConstants.PLAN_TYPE_NONE;
        // make sure object type checks uses this as opposed to getObjectType - necessitated for shared members
        int objectType;
        if (member.isSharedMember()) {
            HspMember baseMember = getBaseMemberOrFailIfExpected(member, actionSet);
            objectType = baseMember.getBaseMemberObjectType();

        } else
            objectType = member.getObjectType();
        switch (objectType) {
        case HspConstants.gObjType_Account: // accounts
            HspAccount accMember = (HspAccount)member;
            HspAccount accParent = (HspAccount)getDimMember(member.getDimId(), member.getParentId());
            if (accParent == null)
                throw new RuntimeException("Invalid Parent Id: " + member.getParentId());


            parentUsedIn = accParent.getUsedIn();
            memberUsedIn = accMember.getUsedIn();
            // Shared member's sourcePlanType will always be saved as NONE no matter what user sets here, so no
            // check is done for it here as it will always be saved as NONE.
            // For Shared Accounts only.
            if (member.isSharedMember())
                memberSourcePlanType = HspConstants.PLAN_TYPE_NONE;
            else
                memberSourcePlanType = accMember.getSrcPlanType();
            break;
        case HspConstants.gObjType_Entity:
            HspEntity entMember = (HspEntity)member;
            HspEntity entParent = (HspEntity)getDimMember(member.getDimId(), member.getParentId());
            if (entParent == null)
                throw new RuntimeException("Invalid Parent Id: " + member.getParentId());


            parentUsedIn = entParent.getUsedIn();
            memberUsedIn = entMember.getUsedIn();
            memberSourcePlanType = HspConstants.PLAN_TYPE_NONE; //entMember.getUsedIn();
            break;

        case HspConstants.gObjType_UserDefinedMember:

            HspMember parent = getDimMember(member.getDimId(), member.getParentId());
            if (parent == null)
                throw new RuntimeException("Invalid Parent Id: " + member.getParentId());

            parentUsedIn = parent.getUsedIn();
            memberUsedIn = member.getUsedIn();
            memberSourcePlanType = HspConstants.PLAN_TYPE_NONE; //entMember.getUsedIn();

            break;
        default:
            return;
            //throw new RuntimeException("Plan Type information unsupported for members of this type, ID: " + member.getId());
        }
        // check parent's selected plan types
        if (!HspMember.areDefinedPlanTypes(parentUsedIn))
            throw new RuntimeException("Undefined plan types specified for parent of member '" + member.getName() + "', ID: " + member.getId());

        // check member's selected plan types
        if (!HspMember.areDefinedPlanTypes(memberUsedIn))
            throw new RuntimeException("Undefined plan types specified for member '" + member.getName() + "', ID: " + member.getId());


        // Don't make this check here, plan types and source plan types are coerced, if necessary, elsewhere
        //if ( (memberUsedIn & parentUsedIn) != memberUsedIn)
        //	throw  new RuntimeException("Selected plan types of member is not a subset of parent's selected plan types, for member '"+member.getName()+"', id: "+member.getId());

        // For accounts only, Make sure Source Plan Type is exactly one of those allowd
        if (objectType == HspConstants.gObjType_Account) {
            if ((!HspMember.isDefinedPlanType(memberSourcePlanType) && !member.isSharedMember()) && (memberSourcePlanType != HspConstants.PLAN_TYPE_NONE))
                throw new RuntimeException("Invalid Source Plan Type for member '" + member.getName() + "', ID: " + member.getId());


            //          Commenting out for Bug# 7298797 - PLANNING SHOULD ALLOW ACCOUNT MEMBERS THAT ARE NOT VALID FOR ANY PLAN TYPES
            //			// Make sure source plan type is ONE OF member's selected plan types, for accounts only
            //			// But ignore the check for account moves, cause we'll force the plan types and source plan type to conform to the
            //			// new parent's later.
            //			if ((memberSourcePlanType & memberUsedIn) != memberSourcePlanType)
            //			{
            //				boolean move = false;
            //				HspMember oldMember = getDimMember(member.getDimId(), member.getId()); // get cached 'old' member
            //				if (oldMember != null)            // assume that if it was found
            //				   move = oldMember.getParentId() != member.getParentId();
            //				if (!move)
            //					throw new HspRuntimeException("MSG_SOURCE_PLAN_TYPE_NOT_IN_VALID_SUBSET", new RuntimeException("The Source Plan Type selected for the member is not in the subset of valid Plan Types; for member Id: " +member.getId()));
            //			}

        }

    }

    private void validateDensitySettings(HspDimension dimension) throws RuntimeException {
        for (int plan = 1; plan <= HspConstants.PLAN_TYPE_ALL; plan = plan << 1) {
            if (!((dimension.getDensity(plan) == HspConstants.kDataDensityDense) || (dimension.getDensity(plan) == HspConstants.kDataDensitySparse))) // density2 on dimension
                throw new RuntimeException("Invalid Density " + plan + " setting for dimension: " + dimension.getName());
        }


    }

    public Vector<HspDimension> getCubeInfo(HspCube cube, int sessionId) throws RuntimeException { //Returns a vector of all dimensions(inclusive of hidden ones
        //that are used in the cube passed in
        hspStateMgr.verify(sessionId);
        if (cube == null)
            throw new RuntimeException("Invalid Cube");


        Vector<HspDimension> cubeInfo = new Vector<HspDimension>();
        Vector<HspDimension> allDims = getBaseDimensions(cube.getPlanType(), sessionId);
        Vector<HspDimension> hiddenDims = getHiddenDimensions(cube.getPlanType(), sessionId);
        if (hiddenDims != null) {
            for (int loop1 = 0; loop1 < hiddenDims.size(); loop1++) {
                HspDimension dim = hiddenDims.elementAt(loop1);
                if (dim != null)
                    allDims.addElement(dim);
            }
        }
        for (int loop1 = 0; loop1 < allDims.size(); loop1++) {
            HspDimension dimsforPlanType = allDims.elementAt(loop1);
            if (dimsforPlanType != null)
                cubeInfo.addElement(dimsforPlanType);
        }
        return cubeInfo;
    }

    public void setDimDensityforPlanType(int planType, int dimId, int densityType, int sessionId) throws Exception { //sets the density of a dimension  for a plantype
        //Lets do some sanity checking first
        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));

        if ((densityType != HspConstants.kDataDensityDense) && (densityType != HspConstants.kDataDensitySparse))
            throw new RuntimeException("Invalid Density Type");
        if (!HspMember.isDefinedPlanType(planType))
            throw new RuntimeException("Invalid Plan Type");

        Vector<HspDimension> allDims = new Vector<HspDimension>();
        allDims = getBaseDimensions(planType, sessionId);
        if (allDims == null)
            throw new RuntimeException("No dimensions returned by getdimension methods");
        Vector<HspDimension> hiddenDims = new Vector<HspDimension>();
        hiddenDims = getHiddenDimensions(planType, sessionId);

        if (hiddenDims != null) {
            for (int loop1 = 0; loop1 < hiddenDims.size(); loop1++) {
                HspDimension dim = hiddenDims.elementAt(loop1);
                if (dim != null)
                    allDims.addElement(dim);
            }
        }

        HspDimension dimensionUd = dimensionCache.getObject(dimId);
        // also try in the hidden dimensionscache
        if (dimensionUd == null) {
            dimensionUd = hiddenDimensionsCache.getObject(dimId);
        }
        //still if we dont find the dimension, then throw an exception
        if (dimensionUd == null)
            throw new RuntimeException("No Dimension returned by dimId ");


        HspDimension dimension = (HspDimension)dimensionUd.cloneForUpdate();

        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        //if the dimension is getting converted to sparse, check if there are atleast two dimension thats is dense
        if (densityType == HspConstants.kDataDensitySparse) {

            int numDenseDims = 0;
            for (int loop1 = 0; loop1 < allDims.size(); loop1++) {
                HspDimension dim = allDims.elementAt(loop1);


                for (int plan = 1; plan <= HspConstants.PLAN_TYPE_ALL; plan = plan << 1) {
                    if (dim.getDensity(plan) == HspConstants.kDataDensityDense)
                        numDenseDims++;
                }
            }
            if (numDenseDims < 2)
                throw new RuntimeException("Atleast one dimesnsion needs to be Dense");
        } else {
            //Only sparse dimensions are supposed to have Custom attributes
            //So if the density is being changed to DENSE, delete all
            //the customattributes for that dimension
            Vector<HspAttributeDimension> attributeDims = getAttributeDimensionsForBaseDim(dimension.getDimId());
            if (attributeDims != null) {
                try {
                    for (int loop1 = 0; loop1 < attributeDims.size(); loop1++) {
                        HspAttributeDimension dim = attributeDims.elementAt(loop1);
                        if (dim != null) {
                            //delete all the attribute dimensions for this base dimension
                            deleteMembers(dim.getDimId(), dim.getId(), true, false, actionSet, sessionId);
                        }
                    }
                } catch (Exception e) {
                    throw new HspRuntimeException(e.getMessage());


                }
            }
        }
        //all checks have passed so set the density
        dimension.setDensity(planType, densityType);

        try {
            saveDimension(dimension, actionSet, sessionId);
            actionSet.doActions();
        } catch (Exception e) {
            HspLogger.LogException(e);
            //if (e instanceof HspCubeRefreshInProgressException) {
            throw (e);
            //} else
            //	throw new RuntimeException(e.getMessage());
        }
    }

    public void setDimPositionforPlanType(int planType, int dimId, int dimDirection, int sessionId) throws Exception { //sets the position of a dimension  for a plantype
        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));

        if ((dimDirection != HspConstants.DIM_MOVE_UP) && (dimDirection != HspConstants.DIM_MOVE_DOWN))
            throw new RuntimeException("Invalid dimension position");
        if (!HspMember.isDefinedPlanType(planType))
            throw new RuntimeException("Invalid Plan Type");

        HspDimension dimensionUd = dimensionCache.getObject(dimId);
        // also try in the hidden dimensionscache
        if (dimensionUd == null) {
            dimensionUd = hiddenDimensionsCache.getObject(dimId);
        }
        //still if we dont find the dimension, then throw an exception
        if (dimensionUd == null)
            throw new RuntimeException("No Dimension returned by dimId ");


        HspDimension dimension = (HspDimension)dimensionUd.cloneForUpdate();

        //After the position change, the position should not be less than or more than
        //number of total dimension
        Vector<HspDimension> allDims = new Vector<HspDimension>();
        allDims = getBaseDimensions(planType, sessionId);
        if (allDims == null)
            throw new RuntimeException("No dimensions returned by getdimension methods");


        Vector<HspDimension> hiddenDims = new Vector<HspDimension>();
        hiddenDims = getHiddenDimensions(planType, sessionId);

        if (hiddenDims != null) {
            for (int loop1 = 0; loop1 < hiddenDims.size(); loop1++) {
                HspDimension dim = hiddenDims.elementAt(loop1);
                if (dim != null)
                    allDims.addElement(dim);
            }
        }

        HspDimPositionComparator dimPositionComparator = new HspDimPositionComparator(planType);
        try {
            HspCSM.sortVector(allDims, dimPositionComparator);
        } catch (Exception e) {
            throw new RuntimeException("Unable to sort Dimensions Vector ");


        }
        //check for invalid moves
        HspDimension tmpDim;
        if (dimDirection == HspConstants.DIM_MOVE_DOWN) {
            tmpDim = allDims.elementAt(allDims.size() - 1);
        } else {
            tmpDim = allDims.elementAt(0);
        }
        //check if Dimension has highest or lowest position already. If yes, throw exception
        if (tmpDim.getId() == dimension.getId())
            throw new RuntimeException("Dimension cannot be moved - " + dimension.getName());


        int dimPosition = 0;
        int swapDimPosition = 0;
        for (int loop1 = 0; loop1 < allDims.size(); loop1++) {
            tmpDim = allDims.elementAt(loop1);
            if (dimension.getId() == tmpDim.getId()) {
                //Swap the positions of the dimensions according to the  move direction
                HspDimension swapDimension = null;
                if (dimDirection == HspConstants.DIM_MOVE_DOWN) {
                    //This case has been checked already, but let's be sure
                    if ((loop1 + 1) < allDims.size())
                        swapDimension = allDims.elementAt(loop1 + 1);
                } else {
                    if ((loop1 - 1) >= 0)
                        swapDimension = allDims.elementAt(loop1 - 1);
                }
                if (swapDimension != null) {
                    try {
                        HspDimension swapDimUpd = (HspDimension)swapDimension.cloneForUpdate();
                        dimPosition = dimension.getPosition(planType);
                        swapDimPosition = swapDimension.getPosition(planType);
                        int start = allDims.size();
                        int lastPos = -1;
                        if (dimPosition == swapDimPosition) {
                            // increment the lower dims position by 1
                            if (dimDirection == HspConstants.DIM_MOVE_DOWN) {
                                swapDimPosition++;
                                start = loop1 + 2;
                                lastPos = swapDimPosition;
                            } else {
                                dimPosition++;
                                start = loop1 + 1;
                                lastPos = dimPosition;
                            }
                        }

                        swapDimUpd.setPosition(planType, dimPosition);
                        dimension.setPosition(planType, swapDimPosition);

                        //Override lock if dim is locked
                        if (swapDimUpd.isObjectLocked()) {
                            swapDimUpd.setOverrideLock(true);
                        }

                        if (dimension.isObjectLocked()) {
                            dimension.setOverrideLock(true);
                        }

                        HspActionSet actionSet = new HspActionSet(hspSQL, user);
                        saveDimension(dimension, actionSet, sessionId);
                        saveDimension(swapDimUpd, actionSet, sessionId);
                        // check if we neeed to renumber the remianing dim positions
                        if (start < allDims.size()) {
                            for (int l = start; l < allDims.size(); l++) {
                                HspDimension lDim = allDims.elementAt(l);
                                int myPos = lDim.getPosition(planType);
                                if (myPos <= lastPos) {
                                    myPos = ++lastPos;
                                    lDim = (HspDimension)lDim.cloneForUpdate();
                                    lDim.setPosition(planType, myPos);
                                    if (lDim.isObjectLocked()) {
                                        lDim.setOverrideLock(true);
                                    }
                                    saveDimension(lDim, actionSet, sessionId);
                                }
                                lastPos = myPos;
                            }
                        }

                        actionSet.doActions();

                    } catch (Exception e) {
                        HspLogger.LogException(e);
                        throw (e);
                    }
                } else {
                    throw new RuntimeException("Dimension with new position does not exist");


                }
            }
        }
    }

    public int deriveValidPlanType(int planTypeOfMember, int planTypeOfMembersParent) throws RuntimeException {
        //  These tests must come in the following order!
        // if parent's plan type value is not valid, just return no-plan-types
        if (!HspMember.areDefinedPlanTypes(planTypeOfMembersParent))
            return HspConstants.PLAN_TYPE_NONE;
        else if (!HspMember.areDefinedPlanTypes(planTypeOfMember))
            return planTypeOfMembersParent; // invalid plan type value specified for member, so return parent's value
        else
            return planTypeOfMember & planTypeOfMembersParent; // return value that is a subset of parent
    }

    public void validateAndSetAccountMemberInformation(HspAccount member) throws RuntimeException {
        validateAndSetAccountMemberInformation(member, null);
    }

    private void validateAndSetAccountMemberInformation(HspAccount member, HspActionSet actionSet) throws RuntimeException { // called on updates and add's

        if (member == null)
            throw new RuntimeException("Invalid member, null");
        //  Account Type    Time Balance					                Variance Reporting	Skip Value					                    Exchange Rate defaults
        //  Expense		Last (balance) or None (flow) or First or Average	Expense		        None or Missing or Zeros or Missing and Zeros		Average
        //  Revenue		Last (balance) or None (flow) or First or Average	Non-Expense	        None or Missing or Zeros or Missing and Zeros		Average
        //  Asset		Last (balance) or None (flow) or First or Average	Non-Expense	        None or Missing or Zeros or Missing and Zeros		Ending
        //  Liability	Last (balance) or None (flow) or First or Average	Non-Expense	        None or Missing or Zeros or Missing and Zeros		Ending
        //  Equity		Last (balance) or None (flow) or First or Average	Non-Expense	        None or Missing or Zeros or Missing and Zeros		Ending
        //  Non-Expense	Last (balance) or None (flow) or First or Average	Non-Expense or Expense	None or Missing or Zeros or Missing and Zeros	None
        //    (saved assumption)
        // Statistical - UNUSED

        // Check Accnount Type AND Variance Reporting properties
        switch (member.getAccountType()) {
        case HspConstants.kDataAccountTypeExpense:
            if (member.getVarianceRep() != HspConstants.kDataVarRepExpense)
                throw new HspRuntimeException("MSG_INVALID_VAR_REPORTING_NOT_EXPENSE"); //"Invalid Variance Reporting value, must be Expense for Expense account types.");
            break;
        case HspConstants.kDataAccountTypeRevenue:
        case HspConstants.kDataAccountTypeAsset:
        case HspConstants.kDataAccountTypeLiability:
        case HspConstants.kDataAccountTypeEquity:
            if (member.getVarianceRep() != HspConstants.kDataVarRepNonExpense)
                throw new HspRuntimeException("MSG_INVALID_VAR_REPORTING_NOT_NONEXPENSE"); //"Invalid Variance Reporting value, must be Non-Expense for Revenue, Asset, Liability, and Equity account types.");
            break;
        case HspConstants.kDataAccountTypeNonexpense: //aka 'Saved Assumptions'
            // validate variance_rep
            if (!(member.getVarianceRep() == HspConstants.kDataVarRepExpense || member.getVarianceRep() == HspConstants.kDataVarRepNonExpense))
                throw new HspRuntimeException("MSG_INVALID_VAR_REPORTING_NOT_EXP_OR_NONEXP"); //"Invalid Variance Reporting, must be Expense or Non-Expense for NonExpense account type.");
            break;
        case HspConstants.kDataAccountTypeStatistical:
            throw new RuntimeException("Statistical Account Type is not currently supported.");

        default:
            throw new HspRuntimeException("MSG_INVALID_ACCOUNT_TYPE");
        }

        // Check that Time Balance is valid (all are valid for all account types)
        if (notInRange(member.getTimeBalance(), HspConstants.kDataTimeBalLow, HspConstants.kDataTimeBalHigh))
            throw new HspRuntimeException("Invalid Time Balance: " + member.getTimeBalance());

        // Check Skip Value (all are valid for all account types)
        if (notInRange(member.getSkipValue(), HspConstants.kDataSkipValLow, HspConstants.kDataSkipValHigh))
            throw new HspRuntimeException("Invalid Skip Value: " + member.getSkipValue());

        // Validate Exchange (currency) Rate AND Data Type properties
        // For Multi-Currency apps:
        //   Exchange Rate MUST be None if Data Type is Non-Currency or Percentage
        //   Exchange Rate MUST NOT be None if Data Type is Currency
        // For Single-Currency app's:
        //   The currency rate must be set to None for all data types.
        if (notInRange(member.getCurrencyRate(), HspConstants.kDataExchangeRateRangeLow, HspConstants.kDataExchangeRateRangeHigh))
            throw new HspRuntimeException("Invalid Exchange (Currency) Rate.");

        // if single currency app currency rate must be none (for all data types)
        if (!hspSystemConfig.isMultiCurrency() && !hspSystemConfig.isSimpleMultiCurrency()) {
            member.setCurrencyRate(HspConstants.kDataExchangeRateNone);
            // RM recommend uncommenting this out and removing the line above and make changes in WF/capex initnializers
            //            if (member.getCurrencyRate() != HspConstants.kDataExchangeRateNone)
            //            {
            //                Properties props = new Properties();
            //                props.setProperty("MBR", member.getMemberName());
            //                throw new HspRuntimeException("MSG_EXCHANGE_RATE_MUST_BE_NONE_SINGLE_CURRENCY_APP", props);
            //            }
        } else // else perform checks for multi-currency app's
        {
            switch (member.getDataType()) {
            case HspConstants.DATA_TYPE_CURRENCY:
                if ((member.getCurrencyRate() == HspConstants.kDataExchangeRateNone) && (hspSystemConfig.isMultiCurrency())) {
                    Properties props = new Properties();
                    props.setProperty("MBR", member.getMemberName());
                    throw new HspRuntimeException("MSG_EXCHANGE_RATE_CANNOT_BE_NONE_IF", props);
                }
                break;
            case HspConstants.DATA_TYPE_NONCURRENCY:
            case HspConstants.DATA_TYPE_PERCENTAGE:
            case HspConstants.DATA_TYPE_ENUMERATION:
            case HspConstants.DATA_TYPE_DATE:
            case HspConstants.DATA_TYPE_TEXT:
                if ((member.getCurrencyRate() != HspConstants.kDataExchangeRateNone) && (hspSystemConfig.isMultiCurrency())) {
                    // set currency rate type to none rather than return an error
                    member.setCurrencyRate(HspConstants.kDataExchangeRateNone);
                    //                        Properties props = new Properties();
                    //                        props.setProperty("MBR", member.getMemberName());
                    //                        throw new HspRuntimeException("MSG_EXCHANGE_RATE_MUST_BE_NONE_IF", props);
                }
                break;
            case HspConstants.DATA_TYPE_UNSPECIFIED:
                // allow any currency rate for now
                break;

            default:
                throw new HspRuntimeException("Invalid Data Type.");
            }
        }

        // Set shared member's data type to base member's
        if (member.isSharedMember()) {
            HspAccount baseMember = (HspAccount)getBaseMemberOrFailIfExpected(member, actionSet);
            member.setDataType(baseMember.getDataType());
            if (baseMember.getDataType() == HspConstants.DATA_TYPE_ENUMERATION)
                member.setEnumerationId(baseMember.getEnumerationId());
            if (baseMember.getSubAccountType() != member.getSubAccountType())
                throw new RuntimeException("The subaccount type of a shared member must match the base member's.");
        }
        // Validate use 445
        // First just make sure it's within the range of any acceptable 445 value
        if (notInRange(member.getUse445(), HspConstants.kDataUse445Low, HspConstants.kDataUse445High))
            throw new RuntimeException("Invalid value for 445 Distribution.");
        HspSystemCfg cfg = hspJS.getSystemCfg();
        if (cfg == null)
            throw new RuntimeException("System Config pointer is null");
        // Now check to see if the 445 value is allright for this application. DontAdjust is always okay,
        // but if the value is not DontAdjust, it must be equal to the system config's setting.
        // if system config value is don't adjust member setting must be dont adjust
        if (cfg.getSupport445() == HspConstants.kData445DontAdjust) {
            if (member.getUse445() != HspConstants.kData445DontAdjust)
                throw new RuntimeException("Invalid use445 setting for member " + member.getName() + ", should be DontAdjust because system config setting is DontAdjust.");
        }
        // else system config value is a 445 setting, so the member 445 setting must be set to the same or dontAdjust
        else {
            if ((member.getUse445() != cfg.getSupport445()) && (member.getUse445() != HspConstants.kData445DontAdjust))
                throw new RuntimeException("Invalid use445 setting for member " + member.getName() + ", not set to DontAdjust or system's 445 setting.");
        }

        // Ensure plan type is set to something valid
        HspAccount parent = (HspAccount)getDimMember(member.getDimId(), member.getParentId(), actionSet);
        if (parent == null)
            throw new RuntimeException("Invalid Parent Id: " + member.getParentId());
        int memberUsedIn = member.getUsedIn();
        member.setUsedIn(deriveValidPlanType(memberUsedIn, parent.getUsedIn()));

        setSourcePlanType(member); // set the source plan type (and make it valid if it isn't currently)
    }

    private void setSourcePlanType(HspAccount member) {
        // The 'UsedIn' property must be valid at this point!
        int memberUsedIn = member.getUsedIn();

        // accounts in the Workforce cube MUST have a source plan type of that cube
        //if (((memberUsedIn & HspConstants.PLAN_TYPE_WF) == HspConstants.PLAN_TYPE_WF) && (member.getSrcPlanType() != HspConstants.PLAN_TYPE_WF) && (!member.isSharedMember()))
        //    throw new HspRuntimeException("Accounts in Workforce must have source plan type set to that cube.");

        // If source plan type specified is 0 or invalid, set it to the first valid (used in) plan type: 1, 2, or 4 in that order
        // If the source plan type is a valid plan type leave it set that way.
        // If there are no 'used in' plan types, set source plan type to 0
        // Set source plan type to 0 for shared members
        if ((memberUsedIn == HspConstants.PLAN_TYPE_NONE) || (member.isSharedMember()))
            member.setSrcPlanType(HspConstants.PLAN_TYPE_NONE); // if member is unassigned, then set source plan type to unassigned
        else {
            if (!HspMember.isDefinedPlanType(member.getSrcPlanType() & memberUsedIn)) {
                for (int plan = 1; plan <= HspConstants.PLAN_TYPE_ALL; plan = plan << 1) {
                    // set source plan type to first used plan type
                    if ((memberUsedIn & plan) == plan) {
                        member.setSrcPlanType(plan);
                        break;
                    }
                }
            }

        }

    }

    private void generateSharedMemberHashtableForSubtree(HspMember parent, Hashtable table) {
        // Note: this method makes use of the fact that shared members (which we're looking
        // for) cannot have kids.
        if (parent == null)
            return;
        Vector children = parent.getChildren();
        if (children != null) {
            for (int loop1 = 0; loop1 < children.size(); loop1++) {
                HspMember child = (HspMember)children.elementAt(loop1);
                if (child == null)
                    continue;
                if (child.hasChildren())
                    generateSharedMemberHashtableForSubtree(child, table);

                if (child.isSharedMember()) {
                    Integer key = child.getBaseMemberId();
                    Object curVal = table.get(key);
                    if (curVal == null) {
                        Vector sharedMembers = new Vector();
                        sharedMembers.addElement(child);
                        table.put(key, sharedMembers);
                    } else {
                        ((Vector)curVal).addElement(child);
                    }
                }

            } //for
        } //if
    }

    public Hashtable generateSharedMemberHashtableForSubtree(HspMember root) {
        Hashtable table = new Hashtable();
        generateSharedMemberHashtableForSubtree(root, table);
        return table;
    }

    private static boolean notInRange(int valueInQuestion, int greaterThanOrEqualTo, int lessThanOrEqualTo) {
        if ((valueInQuestion < greaterThanOrEqualTo) || (valueInQuestion > lessThanOrEqualTo))
            return true;
        return false;
    }

    public void setGenerationValueViaParent(HspMember member, HspMember parent) throws RuntimeException {
        member.setGeneration(parent.getGeneration() + 1);
    }

    private void validateAttributeType(int attributeType) throws RuntimeException {
        // verify that the attribute type is one of the valid types, and that that type is in fact supported
        if (notInRange(attributeType, HspConstants.kDataAttrTypeRangeLow, HspConstants.kDataAttrTypeRangeHigh))
            throw new RuntimeException("Invalid Attribute Type specified");


        /* All the types are supported now.
        //TODO: refine this query when new types supported
		if (attributeType != HspConstants.kDataAttributeText && attributeType != HspConstants.kDataAttributeDate)
			throw  new RuntimeException("Unsupported Attribute Type specified.");
			*/
    }

    public void validateAttributeName(String name) throws RuntimeException {
        // just constrain the length to a maximum of 32 bytes (characters) for DB column, but note,
        // languages like Japanese will be <= 32 characters but > 32 bytes and pass through here
        // so the error will be generated at the DB level.
        // The 32 character name restriction was imposed so the name could be made unique by adding
        // a prefix or a suffix. Since this was never implemented the 32 character is being removed.
        /*
		if (name != null) // null names caught in dimension member name checks
		{
			if (name.length() > AttributeNameMaximumLength)
				throw  new RuntimeException ("Attribute names must be less than " + AttributeNameMaximumLength + " characters in length: " + name);
		}
        */
        // Check the length of the name
        validateDimensionMemberName(name);
    }

    public void validateMemberAttributeBindings(HspMember member) {
        if (member == null)
            throw new HspRuntimeException("Invalid argument, member cannot be null.");


        HspAttributeMemberBinding[] bindings = member.getAttributesToBeSaved();
        // if bindings is null nothing is to be saved so just return;
        if (bindings == null)
            return;
        HspDimension dimension = this.getDimRoot(member.getDimId());
        if (dimension == null)
            throw new HspRuntimeException("Invalid dimension id for member.");


        // loop through and check the bindings one by one
        for (int i = 0; i < bindings.length; i++) {
            HspAttributeMemberBinding binding = bindings[i];
            if (binding == null)
                throw new HspRuntimeException("Attribute binding array element was null.");


            HspAttributeMember attribute = (HspAttributeMember)getAttributeMember(dimension.getId(), binding.getAttributeId());
            // if the attribtute can't be found on this dimension throw an exception
            if (attribute == null)
                throw new HspRuntimeException("Attribute could not be found.");


            // the attribute is okay, now check the perspectives (if any)
            ArrayList<Integer> pIds = getAttributeDimensionPerspectiveDimensions(attribute.getDimId());
            if (pIds != null) {
                if ((pIds.size() > 0) && (pIds.get(0) != null)) {
                    int perspectiveDimId = pIds.get(0);
                    HspMember perspective = getDimMember(perspectiveDimId, binding.getPerspective1());
                    if (perspective == null)
                        throw new HspRuntimeException("Invalid perspective1 id.");


                }
                if ((pIds.size() > 1) && (pIds.get(1) != null)) {
                    int perspectiveDimId = pIds.get(1);
                    HspMember perspective = getDimMember(perspectiveDimId, binding.getPerspective2());
                    if (perspective == null)
                        throw new HspRuntimeException("Invalid perspective2 id.");


                }
            }
        }
    }

    private ArrayList<Integer> getAttributeDimensionPerspectiveDimensions(int attributeDimensionId) {
        ArrayList<Integer> alDimIds = new ArrayList<Integer>();
        HspAttributeDimension attribDim = getAttributeDimension(attributeDimensionId);
        if (attribDim != null) {
            int[] dimIds = attribDim.getPerspectiveDimIds();
            if (dimIds != null) {
                for (int i = 0; i < dimIds.length; i++)
                    alDimIds.add(dimIds[i]);
            }
        }
        return alDimIds;
    }

    public void validateDimensionMemberName(String name) throws InvalidDimensionMemberNameException {
        if (name == null)
            throw new InvalidDimensionMemberNameException(name, InvalidDimensionMemberNameException.ERR_LEADING_WHITE_SPACE);


        int i = 0; // everybody's favorite loop index!

        // Check for leading white space
        for (i = 0; i < whiteSpace.length; i++)
            if (name.startsWith(whiteSpace[i]))
                throw new InvalidDimensionMemberNameException(name, InvalidDimensionMemberNameException.ERR_LEADING_WHITE_SPACE);


        // check for trailing white space
        for (i = 0; i < whiteSpace.length; i++)
            if (name.endsWith(whiteSpace[i]))
                throw new InvalidDimensionMemberNameException(name, InvalidDimensionMemberNameException.ERR_TRAILING_WHITE_SPACE);


        // Check the length of the name
        //TODO: But hey, remember, Essbase restricts to 80 bytes. Limiting to 40 will guarantee ok
        if ((name.length() < DMNameMinimumLength) || (name.length() > DMNameMaximumLength))
            throw new InvalidDimensionMemberNameException(name, InvalidDimensionMemberNameException.ERR_INVALID_LENGTH, DMNameMinimumLength, DMNameMaximumLength);


        // Check to see if the 1st character of the name is valid
        for (i = 0; i < bad1stChars.length; i++)
            if (name.startsWith(bad1stChars[i]))
                throw new InvalidDimensionMemberNameException(name, InvalidDimensionMemberNameException.ERR_INVALID_FIRST_CHARACTER, bad1stChars[i]);


        // Check to see if there are bad characters anywhere in name
        for (i = 0; i < badChars.length; i++)
            if (name.indexOf(badChars[i]) != -1) // -1 ~ didn't occur anywhere in string.
                throw new InvalidDimensionMemberNameException(name, InvalidDimensionMemberNameException.ERR_INVALID_CHARACTER, badChars[i]);


        // check for reserved words
        for (i = 0; i < reservedWords.length; i++)
            if (name.equalsIgnoreCase(reservedWords[i]))
                throw new InvalidDimensionMemberNameException(name, InvalidDimensionMemberNameException.ERR_RESERVED_WORD);


        // Normalize name for hash lookups
        String loweredName = name.toLowerCase();

        // Check for calc script commands
        if (calcScriptCommandsHash.get(loweredName) != null)
            throw new InvalidDimensionMemberNameException(name, InvalidDimensionMemberNameException.ERR_CALC_SCRIPT_COMMAND);


        // Check for report script commands
        if (reportScriptCommandsHash.get(loweredName) != null)
            throw new InvalidDimensionMemberNameException(name, InvalidDimensionMemberNameException.ERR_REPORT_SCRIPT_COMMAND);


        // check if we have a collision with DTS reserved names
        // A collision is OK if DTS member is not enabled
        String dtsGenName = DTSMbrNameToGenHash.get(name);
        if (dtsGenName != null) {
            // found a hit on a DTS member name, check if DTS member is enabled
            if (isDTSMemberEnabled(dtsGenName)) {
                throw new InvalidDimensionMemberNameException(name, InvalidDimensionMemberNameException.MSG_ERR_DTS_NAME_CONFLICT);


            }
        }

        String dtsMbrName = DTSGenToMbrNameHash.get(loweredName);
        if (dtsMbrName != null) {
            // found a hit on a DTS gen, check if DTS member is enabled
            if (isDTSMemberEnabled(dtsMbrName)) {
                throw new InvalidDimensionMemberNameException(name, InvalidDimensionMemberNameException.MSG_ERR_DTS_NAME_CONFLICT);


            }
        }

    }

    private void validateDataStorageWith2PassSetting(HspMember member) {
        // The rules for 2 pass calc and data storage: 2 pass calc is valid for any value of data
        // storage on accounts, otherwise, data storage must be set to dynamic calc or dynamic calc
        // and store (for entities and user defined members).  (Essbase allows 2 pass calc to be set to
        // true for any value of data store on entities and user defined members but ignores it for
        // values other than dynamic calc's.)
        int dataStorage = member.getDataStorage();
        //boolean twoPassCalc = member.getTwopassCalc();
        //validateDataStorage(member);

        HspMemberPTProps[] mbrProps = member.getMemberPTPropsToBeSaved();
        int nTimes = 0;
        if (mbrProps != null)
            nTimes = mbrProps.length;

        for (int i = -1; i < nTimes; i++) {

            if (i > -1) {
                HspMemberPTProps mbrProp = mbrProps[i];
                dataStorage = mbrProp.getDataStorage();
            }
            // once data storage has been validated for accounts the two pass calc setting can
            // can be any value for non-shareds, for shareds it should be false.
            if (member.getDimId() == HspConstants.kDimensionAccount) {
                if ((member.getTwopassCalc()) && (dataStorage == HspConstants.kDataStorageSharedMember))
                    throw new RuntimeException("Two pass calc cannont be set to true for shared members, for member: " + member.getName());


                else
                    return;
            }
            switch (dataStorage) {
                // for non-accounts store, neverShare, and shareds, two pass calc must be false
            case HspConstants.kDataStorageStoreData:
            case HspConstants.kDataStorageNeverShare:
            case HspConstants.kDataStorageLabelOnly:
                if (member.getTwopassCalc()) {
                    //throw  new RuntimeException("For non-accounts data storage setting of store, never share, or label only two pass calc must be false - name: "+member.getName()+", id: "+member.getId());
                    Properties p = new Properties();
                    p.put("MEMBER_NAME", member.getName());
                    throw new HspRuntimeException("MSG_NO_2PASS_WITH_STORE", p);
                }
                break;
            case HspConstants.kDataStorageSharedMember:
                if (!supportsSharedMembers(member.getDimId())) {
                    HspDimension dim = getDimRoot(member.getDimId());
                    throw new RuntimeException("Shared Members not supported for Dimension: " + dim.getName() + ", member name: " + member.getName());
                }
                if (member.getTwopassCalc())
                    throw new RuntimeException("Two pass calc must be set to false for shared members, member: " + member.getName());


                break;
            case HspConstants.kDataStorageDynamic:
            case HspConstants.kDataStorageDynCalcStore:
                return; // any value ok for two pass calc
            default:
                throw new RuntimeException("Inconsistent Data Storage and Two Pass Calculation settings for member - name: " + member.getName());


            }
        }
    }

    public boolean isValidScenario(HspScenario oScenario, int sessionId) throws RuntimeException { //Check for SessionID
        hspStateMgr.verify(sessionId);

        if (oScenario == null)
            throw new RuntimeException("Invalid scenario: null");


        // validate name of Scenario
        //As scenario is a dimension in Essbase, we can use the method of
        //checking dimension names to check name of scenario
        try {
            if (oScenario.getDataStorage() != HspConstants.kDataStorageSharedMember)
                validateDimensionMemberName(oScenario.getName());
        } catch (InvalidDimensionMemberNameException e) {
            return (false);
        }

        //Validate years and timeperiods
        //year data fetch
        HspYear startYear = yearCache.getObject(oScenario.getStartYrId());
        HspYear endYear = yearCache.getObject(oScenario.getEndYrId());
        //time period data fetch
        HspTimePeriod startTimePeriod = timePeriodCache.getObject(oScenario.getStartTpId());
        HspTimePeriod endTimePeriod = timePeriodCache.getObject(oScenario.getEndTpId());

        if (startYear == null) {
            throw new RuntimeException("StartYear not found");
        }
        if (endYear == null) {
            throw new RuntimeException("EndYear not found");
        }
        if (startTimePeriod == null) {
            throw new RuntimeException("StartTimePeriod not found");
        }
        if (endTimePeriod == null) {
            throw new RuntimeException("EndTimePeriod not found");
        }
        /*
        Modifying year checks to use position rather than object ID since object IDs may not
        be in orser.

        An assumptions is made here:(as it stands now in Planning)
			1.The ids of the yeard generated in a consecutive manner
			(e.g.) if firstYear is 2000 and numYears is 5 and
			2000==id1, 2001 ==id2, 2002=id3,....2004==id5, then id5>id4>id3>id2>id1.
			The maximum number of years is 15.
			(e.g.) if firstYear is 2000 and numYears can be a maximum of 15,
			so the lastYear is <= 2014
		*/
        double startYearPos = startYear.getPosition();
        double endYearPos = endYear.getPosition();

        if (startYearPos == endYearPos) {
            if (startTimePeriod.period > endTimePeriod.period) {
                throw new HspRuntimeException("MSG_YRPD_START_GRTR_END");
            }
        } else if (startYearPos > endYearPos) {
            throw new HspRuntimeException("MSG_YRPD_START_GRTR_END");
        }

        //Check for FxTableValidity
        HspFXTable fxTable = hspCurDB.GetFXTable(sessionId, oScenario.getFxTbl());

        if (fxTable == null) {
            throw new RuntimeException("fxTable not found");


        }
        //We have tried the scenario for all invalid cases, so let this one get through
        return (true);
    }

    public void validateCurrency(HspCurrency currency, int sessionId) throws Exception { //Check for SessionID
        hspStateMgr.verify(sessionId);
        if (currency == null)
            throw new IllegalArgumentException("currency arugument is null.");


        HspCurDBImpl.validateSymbol(currency.getSymbol());
        HspCurDBImpl.validateType(currency.getCurrencyType());
        HspCurDBImpl.validateScale(currency.getScale());
        HspCurDBImpl.validateThousandsSeparator(currency.getThousandsSeparator());
        HspCurDBImpl.validateDecimalSeparator(currency.getDecimalSeparator());
        HspCurDBImpl.validateNegativeStyle(currency.getNegativeStyle());
        HspCurDBImpl.validateNegativeColor(currency.getNegativeColor());
        //The thousands separator and the decimal marker should not be the same
        if ((currency.getThousandsSeparator() == HspConstants.PREF_THOUSANDS_COMMA) && (currency.getDecimalSeparator() == HspConstants.PREF_DECIMAL_COMMA))
            throw new RuntimeException("Currency Thousand Separator and Decimal separator cannot be the same");

        if ((currency.getThousandsSeparator() == HspConstants.PREF_THOUSANDS_POINT) && (currency.getDecimalSeparator() == HspConstants.PREF_DECIMAL_POINT))
            throw new RuntimeException("Currency Thousand Separator and Decimal separator cannot be the same");


        // make sure the triangulation id, if specified, is valid
        int triangulationCurrencyId = currency.getTriangulationCurrencyID();
        if (triangulationCurrencyId > 0) {
            HspCurrency triangulationCurrency = hspCurDB.getCurrency(triangulationCurrencyId);
            if (triangulationCurrency == null)
                throw new RuntimeException("Invalid triangulation currency id " + triangulationCurrencyId + " specified for currency.");


            // if the specified triangulation currency is triangulated throw an exception.
            if (triangulationCurrency.getTriangulationCurrencyID() > 0)
                throw new RuntimeException("The specified triangulation currency is a triangulated currency.");


            // we have a valid triangulation currency
            // the default currency cannot be triangulated, throw an exception if it is
            Vector<HspCurrency> v = hspCurDB.getDefaultCurrency(sessionId);
            HspCurrency defaultCurrency = null;
            if ((v != null) && (v.size() == 1)) {
                defaultCurrency = v.firstElement();
                if (defaultCurrency == null)
                    throw new RuntimeException("Unable to retrieve default currency.");
            } else {
                throw new RuntimeException("Unable to retrieve default currency.");
            }
            if (currency.getId() == defaultCurrency.getId()) // triangulating the default currency is not allowed
                throw new RuntimeException("The application default currency cannot be triangulated.");
            if (hspCurDB.isTriangulated(currency, sessionId))
                throw new RuntimeException("A triangulation currency cannot be triangulated.");


        }

    }

    private void validateYear(HspYear year) throws RuntimeException {
        if (year.getObjectType() != HspConstants.gObjType_Year)
            throw new RuntimeException("Invalid object type for year.");


        if (!hspCalDB.hasAllYearsParent()) {
            if (year.getParentId() != HspConstants.gObjDim_Year)
                throw new RuntimeException("Invalid parent Id for year");

            Vector<HspCube> cubes = getCubes(internalSessionId);

            for (HspCube cube : cubes) {
                if (cube.getType() == HspConstants.ASO_CUBE) {
                    if (year.getConsolOp(cube.getPlanType()) != HspConstants.kDataConsolIgnore && year.getConsolOp(cube.getPlanType()) != HspConstants.kDataConsolAddition)
                        year.setConsolOp(cube.getPlanType(), HspConstants.kDataConsolAddition);
                    //throw  new RuntimeException("Consolidation operators for years must be set to Ignore or Addition for ASO plan types.");
                } else {
                    if (!year.getName().equals(HspSetupConsts.sAllYears) && year.getConsolOp(cube.getPlanType()) != HspConstants.kDataConsolIgnore)
                        year.setConsolOp(cube.getPlanType(), HspConstants.kDataConsolIgnore);
                    //throw  new RuntimeException("Consolidation operators for years must be set to Ignore for all plan types.");

                    if (year.getName().equals(HspSetupConsts.sAllYears) && year.getConsolOp(cube.getPlanType()) != HspConstants.kDataConsolAddition)
                        year.setConsolOp(cube.getPlanType(), HspConstants.kDataConsolAddition);
                    //throw  new RuntimeException("Consolidation operators for All Years must be set to Addition for all plan types.");

                }
            }
        } else {
            //        if (year.getParentId() != hspCalDB.getAllYearsParent().getId())
            //            throw  new RuntimeException("Invalid parent Id for year");

            /*
         * Only the members which are children of All Year should inherit the console_op property from parent. Siblings should retain their own property.
         * */
            if (year.getParentId() == hspCalDB.getAllYearsParent().getId()) {
                for (int i = 1; i <= HspConstants.PLAN_TYPE_ALL; i = i << 1) {
                    if (year.getConsolOp(i) != HspConstants.kDataConsolAddition)
                        year.setConsolOp(i, HspConstants.kDataConsolAddition);
                    //throw  new RuntimeException("Consolidation operators for years must be set to Addition for all plan types.");
                }
            }
        }
    }

    private int getDformatDateFormat(SimpleDateFormat df) {
        HspUtils.verifyArgumentNotNull(df, "dateFormat df");
        String p = df.toPattern();
        if (p.equalsIgnoreCase("M/d/yy"))
            return 0;
        else if (p.equalsIgnoreCase("dd/MM/yy"))
            return 1;
        else if (p.equalsIgnoreCase("yy-M-d"))
            return 2;
        return -1;
    }

    private String getDateFormatStringFromDateFormat(SimpleDateFormat df) {
        HspUtils.verifyArgumentNotNull(df, "dateFormat df");
        String p = df.toPattern();
        if (p.equalsIgnoreCase("M/d/yy"))
            return HspConstants.ATTR_DATE_FMT_MM_DD_YYYY;
        else if (p.equalsIgnoreCase("dd/MM/yy"))
            return HspConstants.ATTR_DATE_FMT_DD_MM_YYYY;
        else if (p.equalsIgnoreCase("yy-M-d"))
            return HspConstants.ATTR_DATE_FMT_YYYY_MM_DD;
        return null;
    }

    public String validateDateString(String date) throws RuntimeException {
        return validateDateString(date, null);
    }

    public String validateDateString(String date, DateFormat dateFormatOverride) throws RuntimeException {
        String dateFormat = null;
        int dFormat = 0;
        if (dateFormatOverride == null) {
            HspSystemCfg hspSystemCfg = hspJS.getSystemCfg();
            dateFormat = hspSystemCfg.getDateFmt();
            if ((dateFormat == null) || (dateFormat.equals(HspConstants.ATTR_DATE_FMT_MM_DD_YYYY)))
                dFormat = 0;
            else if (dateFormat.equals(HspConstants.ATTR_DATE_FMT_DD_MM_YYYY))
                dFormat = 1;
            else if (dateFormat.equals(HspConstants.ATTR_DATE_FMT_YYYY_MM_DD))
                dFormat = 2;
            else
                throw new HspRuntimeException("Date format \"" + dateFormat + "\" not currently supported.");


        } else {
            dateFormat = getDateFormatStringFromDateFormat((SimpleDateFormat)dateFormatOverride);
            if (dFormat == -1)
                throw new HspRuntimeException("Overriden date format \"" + dateFormat + "\"not supported.");


            dFormat = getDformatDateFormat((SimpleDateFormat)dateFormatOverride);
        }

        int month = 0;
        int day = 0;
        int year = 0;
        String firstToken = null;
        String secondToken = null;
        String thirdToken = null;
        Properties p = new Properties();
        StringTokenizer st = new StringTokenizer(date, "-");
        if (st.countTokens() != 3) {
            p.put("DATE_FORMAT", dateFormat);
            throw new HspRuntimeException("MSG_DATE_STRING_INCORRECTLY_FORMATTED", p);


        }
        try {
            // this method works fine with superfluous leading 0's but essbase doesn't like them so now we check.
            // we assume mo-day-year, but will switch both below for validateion if the format is day-month-year
            firstToken = st.nextToken();
            if ((dFormat != 2) && (firstToken.length() > 2))
                throw new NumberFormatException("Invalid Month/Day length");
            if ((dFormat == 2) && ((firstToken.length() > 4) || (firstToken.length() < 4)))
                throw new NumberFormatException("Invalid Year length");


            secondToken = st.nextToken();
            if (secondToken.length() > 2)
                throw new NumberFormatException("Invalid Month/Day length");


            thirdToken = st.nextToken();
            if ((dFormat != 2) && ((thirdToken.length() > 4) || (thirdToken.length() < 4)))
                throw new NumberFormatException("Invalid Year length");
            if ((dFormat == 2) && (thirdToken.length() > 2))
                throw new NumberFormatException("Invalid Day length");


        } catch (NumberFormatException e) {
            p.put("DATE_FORMAT", dateFormat);
            throw new HspRuntimeException("MSG_DATE_STRING_INCORRECTLY_FORMATTED", p);


        }
        switch (dFormat) {
        case 0:
            month = new Integer(firstToken);
            day = new Integer(secondToken);
            year = new Integer(thirdToken);
            break;
        case 1:
            month = new Integer(secondToken);
            day = new Integer(firstToken);
            year = new Integer(thirdToken);
            break;
        case 2:
            month = new Integer(secondToken);
            day = new Integer(thirdToken);
            year = new Integer(firstToken);
            break;
        }
        month--; // months are zero based
        if ((year < 1900) || (year > 9999)) {
            p.put("DATE_FORMAT", dateFormat);
            throw new HspRuntimeException("MSG_DATE_STRING_INCORRECTLY_FORMATTED", p);


        }
        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.YEAR, year);
        if (month < calendar.getActualMinimum(Calendar.MONTH) || month > calendar.getActualMaximum(Calendar.MONTH)) {
            p.put("DATE_STRING", date);
            throw new HspRuntimeException("MSG_DATE_STRING_FIELD_OUT_OF_RANGE", p);


        }
        calendar.set(Calendar.MONTH, month);
        if (day < calendar.getActualMinimum(Calendar.DAY_OF_MONTH) || day > calendar.getActualMaximum(Calendar.DAY_OF_MONTH)) {
            p.put("DATE_STRING", date);
            throw new HspRuntimeException("MSG_DATE_STRING_FIELD_OUT_OF_RANGE", p);


        }

        // Return month and day zero padded (because essbase wants it that way)
        if ((dFormat != 2) && (firstToken.length() < 2))
            firstToken = "0" + firstToken;
        if (secondToken.length() < 2)
            secondToken = "0" + secondToken;

        // return a zero padded string
        return new String(firstToken + "-" + secondToken + "-" + thirdToken);

    }

    private void validateTimePeriod(HspTimePeriod timePeriod, HspMember parent) throws RuntimeException {
        //TODO: how do we determine beginning balance? Used to be via name on desktop
        //TODO: set the following consolidation op's to ignore if this is a beginning balance
        //timePeriod.setConsolOp1(HspConstants.kDataConsolAddition);
        //timePeriod.setConsolOp2(HspConstants.kDataConsolAddition);
        //timePeriod.setConsolOp4(HspConstants.kDataConsolAddition);
        //timePeriod.setDataStorage(HspConstants.kDataStorageStoreData);
        switch (timePeriod.getType()) {
        case HspConstants.YEAR_TP_TYPE:
        case HspConstants.ROLLUP_TP_TYPE:
            break;
        case HspConstants.LEAF_TP_TYPE:
            break;
        case HspConstants.ALTERNATE_TP_TYPE:
        case HspConstants.DTS_TP_TYPE:
            break;
        default:
            throw new RuntimeException("Invalid time period type: " + timePeriod.getType());


        }

        // Checks for alternate time periods and DTS time period parents
        // for alternate tp's parent must be Period dimension root or another alternate tp
        // for DTS periods parent must be Period Dimension or another DTS period
        if (isAlternateTimePeriod(timePeriod)) {
            if (parent.getId() == HspConstants.kDimensionTimePeriod)
                ; // okay if parent is tp dimension
            else if (parent.getObjectType() == HspConstants.gObjType_Period) { // else if parent is a timePeriod it must be an alternate type
                HspTimePeriod parentTimePeriod = (HspTimePeriod)parent;
                if (!isAlternateTimePeriod(parentTimePeriod))
                    throw new RuntimeException("An alternate time period can only be added under the dimension root or another alternamte time period.");
            } else
                throw new RuntimeException("An alternate time period can only be added under the dimension root or another alternamte time period.");


        } else if (isDTSTimePeriod(timePeriod)) {
            if (parent.getId() == HspConstants.kDimensionTimePeriod) {
                // Make sure DTS member has same usedIn asusedIn as parent when enabled
                if (timePeriod.getDTSGeneration() > 0)
                    timePeriod.setUsedIn(parent.getUsedIn());
            } else if (parent.getObjectType() == HspConstants.gObjType_Period) { // else if parent is a timePeriod it must be an alternate type
                HspTimePeriod parentTimePeriod = (HspTimePeriod)parent;
                if (!isAlternateTimePeriod(parentTimePeriod))
                    throw new RuntimeException("An DTS time period can only be added under the dimension root or another DTS time period.");
            } else
                throw new RuntimeException("An DTS time period can only be added under the dimension root or another DTS time period.");


            // check DTS member for a member name collision
            String dtsGenName = DTSMbrNameToGenHash.get(timePeriod.getMemberName());
            if (dtsGenName != null && timePeriod.getDTSGeneration() > 0) {
                HspMember mbr = this.getMemberByName(dtsGenName);
                if (mbr != null) {
                    throw new InvalidDimensionMemberNameException(mbr.getMemberName(), InvalidDimensionMemberNameException.MSG_ERR_DTS_NAME_CONFLICT);
                }
            }
        }

        // we only support shared's for alternate and dts members. But note that the shared alternate tp type
        // may have an alternate or other tp base
        if (timePeriod.getDataStorage() == HspConstants.kDataStorageSharedMember)
            if (!(isAlternateTimePeriod(timePeriod)) || (isDTSTimePeriod(timePeriod)))
                throw new RuntimeException("Only alternate and dts time periods can be shared members (but base can be of any time period type.");


        // Don't need to set 2 pass calc to true for summary time periods because
        // roll-up will occur in calc script.
        //1-408966601
        //if (timePeriod.getTwopassCalc())
        //	throw new RuntimeException("Two Pass Calc must be set to false for time periods.");
    }

    private boolean isAlternateTimePeriod(HspTimePeriod timePeriod) {
        HspUtils.verifyArgumentNotNull(timePeriod, "timePeriod");
        if (timePeriod.getType() == HspConstants.ALTERNATE_TP_TYPE)
            return true;
        return false;
    }

    private boolean isDTSTimePeriod(HspTimePeriod timePeriod) {
        HspUtils.verifyArgumentNotNull(timePeriod, "timePeriod");
        if (timePeriod.getType() == HspConstants.DTS_TP_TYPE)
            return true;
        return false;
    }

    public void updateCalendarPositionsFromYearTotal(int sessionId) throws Exception {
        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        updateCalendarPositions(hspCalDB.getYearTotal(sessionId), actionSet, sessionId);
        actionSet.doActions();
    }

    private void updateCalendarPositions(HspMember member, HspActionSet actionSet, int sessionId) throws Exception {
        if (member.getDimId() == HspConstants.kDimensionTimePeriod) {
            // only update if we modified an time period from base hierarchy
            HspTimePeriod timePeriod = (HspTimePeriod)member;
            if ((timePeriod.getType() == HspConstants.LEAF_TP_TYPE) || (timePeriod.getType() == HspConstants.ROLLUP_TP_TYPE) || (timePeriod.getType() == HspConstants.YEAR_TP_TYPE)) {
                // reorder calendar node positions expected to what's expected by desktop acm - someday this will be unnecessary
                actionSet.doActions(false);
                hspCalDB.updateCalendarPositions(timePeriod, actionSet, sessionId);
            }
        }
    }


    public boolean isValidVersion(HspVersion oVersion, int sessionId) throws RuntimeException { //Check for SessionID
        hspStateMgr.verify(sessionId);

        if (oVersion == null)
            throw new RuntimeException("Invalid Version: null");


        // validate name of Version
        //As Version is a dimension in Essbase, we can use the method of
        //checking dimension names to check name of scenario
        try {
            if (oVersion.getDataStorage() != HspConstants.kDataStorageSharedMember)
                validateDimensionMemberName(oVersion.getName());
        } catch (InvalidDimensionMemberNameException e) {
            return (false);
        }

        //Validate Version type
        int thisVersionType = oVersion.getVersionType();
        if ((thisVersionType != HspConstants.VERSION_OFFICIAL_TARGET) && (thisVersionType != HspConstants.VERSION_OFFICIAL_BU))
            throw new RuntimeException("Version type not supported");


        //We have not tried to see if it is a personal version as it is no longer used
        //if (thisVersionType != HspConstants.VERSION_PERSONAL) throw new RuntimeException("Version type not supported");

        //We have tried the version for all invalid cases, so let this one get through
        return (true);
    }

    /*	public double newPosition(HspMember member)
	{
	 // Returns a position value to be used by a new child when it is being added to the
	 // specified parent.  If the parent has no children, the parent's position value is returned.
	 // If the parent has children, the maximum position value of all children, plus 1, is returned.
	 // For now, this method is a functional duplicate of it's original planning counterpart.
		return getParentLastPosition(member) + 1.0;

	}
*/

    private void validateMetric(HspMetric metric) throws RuntimeException {
        // TODO: Add metric specific validations here.
    }

    private void validateReplacementMember(HspReplacementMember replacementMember) throws RuntimeException {
        // TODO: Add metric specific validations here.
    }

    /* private double getLastChildPosition(HspMember parent)
	{
	 // If the parent has no children, the parent's position value is returned.
	 // If the parent has children, the maximum position value of all children, plus 1, is returned.
	 // For now, this method is a functional duplicate of it's original planning counterpart.

		if (parent == null)
			throw new RuntimeException("Invalid Parent Id: " + parent.getId());






		Vector children = parent.getChildren();
		if (children != null && children.size() != 0)
		{
			// vector is presorted so last is guaranteed to be greatest position value
			return (((HspMember)children.lastElement()).getPosition());
		}
		else
		{
			return parent.getPosition();
		}

	} */

    public int getDefaultCurrencyRate(int accountType) {
        switch (accountType) {
        case HspConstants.kDataAccountTypeRevenue:
        case HspConstants.kDataAccountTypeExpense:
            return HspConstants.kDataExchangeRateAverage;
        case HspConstants.kDataAccountTypeAsset:
        case HspConstants.kDataAccountTypeLiability:
        case HspConstants.kDataAccountTypeEquity:
            return HspConstants.kDataExchangeRateEnding;
        case HspConstants.kSavedAssumptionType:
            return HspConstants.kDataExchangeRateNone;
        default:
            throw new RuntimeException("Unknown account type: " + accountType);


        }
    }

    public int getDefaultTimeBalance(int accountType) {
        switch (accountType) {
        case HspConstants.kDataAccountTypeRevenue:
        case HspConstants.kDataAccountTypeExpense:
            return HspConstants.kDataTimeBalNone;
        case HspConstants.kDataAccountTypeAsset:
        case HspConstants.kDataAccountTypeLiability:
        case HspConstants.kDataAccountTypeEquity:
            return HspConstants.kDataTimeBalLast;
        case HspConstants.kSavedAssumptionType:
            return HspConstants.kDataTimeBalNone;
        default:
            throw new RuntimeException("Unknown account type: " + accountType);


        }
    }

    private void validateSharedWillBeUniqueSharedUnderParent(HspMember member, HspActionSet actionSet, int sessionId) throws Exception {
        Vector<HspMember> shared = getSharedMembersOfBase(member.getDimId(), member.getBaseMemberId(), actionSet, sessionId);
        if (shared != null) {
            for (int i = 0; i < shared.size(); i++) {
                HspMember sharedMember = shared.elementAt(i);
                if (sharedMember != null) {
                    if (sharedMember.getParentId() == member.getParentId())
                        throw new HspRuntimeException("MSG_2_SHAREDS_SAME_PARENT", new RuntimeException("Unable to add shared member " + member.getObjectName() + " because a shared member for the same base member already exists under parent."));


                }
            }
        }
    }

    private void validateBaseWontBeSiblingOfShared(HspMember member, HspActionSet actionSet, int sessionId) throws Exception {
        // This method is meant to be called on a move of a non-shared member. It checks to see that this member being moved
        // will not be moved under a parent that already has a child that is a shared of the member being moved.

        // we're only concerned with potential base members
        if (member.getDataStorage() == HspConstants.kDataStorageSharedMember)
            return;
        // see if the base has any shareds
        Vector<HspMember> shareds = getSharedMembersOfBase(member.getDimId(), member.getId(), actionSet, sessionId);
        if ((shareds == null) || (shareds.size() < 1))
            return; // no shareds for this member, so return

        // Now see if any of the shareds have the same parentid as the base
        if (shareds != null) {
            for (int i = 0; i < shareds.size(); i++) {
                HspMember sharedMember = shareds.elementAt(i);
                if (sharedMember != null) {
                    if (sharedMember.getParentId() == member.getParentId())
                        throw new HspRuntimeException("MSG_MOVE_BASE_PARENT_HAS_SHARED", new RuntimeException("Unable to move base member " + member.getObjectName() + " because a shared member for this member already exists under parent."));


                }
            }
        }
    }

    public boolean isNotSortableMemberType(int objectType) {
        switch (objectType) {
        case HspConstants.gObjType_Account:
        case HspConstants.gObjType_Entity:
            //case HspConstants.gObjType_AttributeDim:
        case HspConstants.gObjType_AttributeMember:
            //case HspConstants.gObjType_SharedMember:
        case HspConstants.gObjType_UserDefinedMember:
        case HspConstants.gObjType_Year:
        case HspConstants.gObjType_Period:
        case HspConstants.gObjType_Currency:
        case HspConstants.gObjType_CurrencyMember:
        case HspConstants.gObjType_Version:
        case HspConstants.gObjType_Scenario:
        case HspConstants.gObjType_DPDimMember:
        case HspConstants.gObjType_BudgetRequest:
        case HspConstants.gObjType_BudgetRequestASO:
        case HspConstants.gObjType_DecisionPackageASO:
            //case HspConstants.gObjType_SimpleCurrency:
        case HspConstants.gObjType_ReplacementMember:
        case HspConstants.gObjType_Metric:
            return false;
        default:
            return true;
        }
    }

    public boolean isNotMemberObjectType(int objectType) {
        //TODO: get rid of this methond if other checks take care of this
        switch (objectType) {
        case HspConstants.gObjType_Account:
        case HspConstants.gObjType_Entity:
        case HspConstants.gObjType_AttributeDim:
        case HspConstants.gObjType_AttributeMember:
        case HspConstants.gObjType_SharedMember:
        case HspConstants.gObjType_UserDefinedMember:
        case HspConstants.gObjType_Currency:
        case HspConstants.gObjType_CurrencyMember:
        case HspConstants.gObjType_Scenario:
        case HspConstants.gObjType_Version:
        case HspConstants.gObjType_Period:
        case HspConstants.gObjType_Year:
        case HspConstants.gObjType_DPDimMember:
        case HspConstants.gObjType_BudgetRequest:
        case HspConstants.gObjType_DecisionPackageASO:
        case HspConstants.gObjType_BudgetRequestASO:
            //case HspConstants.gObjType_SimpleCurrency:
        case HspConstants.gObjType_ReplacementMember:
        case HspConstants.gObjType_Metric:
            return false;
        default:
            return true;
        }

    }

    public List<HspPlanningUnit> getReferencingStartedPUs(HspMember member, List<HspPlanningUnit> pusUsedIn, int maxListSize, int sessionId) {
        // currently only scenario, version, and entity members have the potential to retun an non-empty list.
        hspStateMgr.verify(sessionId);
        if (member == null)
            throw new RuntimeException("Member argument cannot be null.");


        // If a list was passed in, append to it; else if no list was passed in (null) create a new one, append to it
        // as required, then return it.
        if (pusUsedIn == null)
            pusUsedIn = new ArrayList<HspPlanningUnit>();
        if ((maxListSize > 0) && (pusUsedIn.size() >= maxListSize))
            return pusUsedIn;

        switch (member.getObjectType()) {
        case HspConstants.gObjType_Entity:
            return getReferencingStartedPUs((HspEntity)member, pusUsedIn, maxListSize, sessionId);
        case HspConstants.gObjType_Scenario:
            return getReferencingStartedPUs((HspScenario)member, pusUsedIn, maxListSize, sessionId);
        case HspConstants.gObjType_Version:
            return getReferencingStartedPUs((HspVersion)member, pusUsedIn, maxListSize, sessionId);
        }
        return pusUsedIn;
    }

    private List<HspPlanningUnit> getReferencingStartedPUs(HspEntity entity, List<HspPlanningUnit> pusUsedIn, int maxListSize, int sessionId) {
        hspStateMgr.verify(sessionId);
        if (entity == null)
            throw new RuntimeException("Entity argument cannot be null.");


        // If a list was passed in, append to it; else if no list was passed in (null) create a new one, append to it
        // as required, then return it.
        if (pusUsedIn == null)
            pusUsedIn = new ArrayList<HspPlanningUnit>();
        if ((maxListSize > 0) && (pusUsedIn.size() >= maxListSize))
            return pusUsedIn;

        Vector<HspVersion> versions = hspPMDB.getPMVersions(sessionId);
        Vector<HspScenario> scenarios = hspPMDB.getPMScenarios(sessionId);
        if ((versions != null) && (scenarios != null)) {
            for (int loop1 = 0; loop1 < versions.size(); loop1++) {
                HspVersion tmpVersion = versions.elementAt(loop1);
                for (int loop3 = 0; loop3 < scenarios.size(); loop3++) {
                    HspScenario tmpScenario = scenarios.elementAt(loop3);
                    getStartedPlanningUnitsWithEntities(pusUsedIn, maxListSize, tmpScenario.getId(), tmpVersion.getId(), 0, entity.getId(), sessionId);

                    List<HspDPMember> dps = getDecisionPackages(sessionId);
                    for (HspDPMember dp : dps) {
                        getStartedPlanningUnitsWithEntities(pusUsedIn, maxListSize, tmpScenario.getId(), tmpVersion.getId(), dp.getId(), entity.getId(), sessionId);
                    }
                }
            }
        }
        return pusUsedIn;
    }

    private void getStartedPlanningUnitsWithEntities(List<HspPlanningUnit> pusUsedIn, int maxListSize, int scenarioId, int versionId, int dpMemberId, int entityIdToCompare, int sessionId) {
        Vector<HspPlanningUnit> allPUs = hspPMDB.getPlanningUnits(scenarioId, versionId, dpMemberId, sessionId);
        if (allPUs != null) {
            for (int loop2 = 0; loop2 < allPUs.size() && ((maxListSize < 0) || (pusUsedIn.size() < maxListSize)); loop2++) {
                HspPlanningUnit thisPU = allPUs.elementAt(loop2);
                if ((thisPU.getProcessState() != HspConstants.PU_NOT_STARTED) && ((entityIdToCompare == 0) || (entityIdToCompare > 0 && thisPU.getEntityId() == entityIdToCompare))) {
                    pusUsedIn.add(thisPU);
                }
            }
        }
    }

    public List<HspPlanningUnit> getReferencingStartedPUs(HspScenario scenario, List<HspPlanningUnit> pusUsedIn, int maxListSize, int sessionId) {
        hspStateMgr.verify(sessionId);
        if (scenario == null)
            throw new RuntimeException("Scenario argument cannot be null.");


        // If a list was passed in, append to it; else if no list was passed in (null) create a new one, append to it
        // as required, then return it.
        if (pusUsedIn == null)
            pusUsedIn = new ArrayList<HspPlanningUnit>();
        if ((maxListSize > 0) && (pusUsedIn.size() >= maxListSize))
            return pusUsedIn;

        Vector<HspVersion> versions = hspPMDB.getPMVersions(sessionId);
        if (versions != null) {
            for (int loop1 = 0; loop1 < versions.size(); loop1++) {
                HspVersion tmpVersion = versions.elementAt(loop1);
                getStartedPlanningUnitsWithEntities(pusUsedIn, maxListSize, scenario.getId(), tmpVersion.getId(), 0, 0, sessionId);
                List<HspDPMember> dps = getDecisionPackages(sessionId);
                for (HspDPMember dp : dps) {
                    getStartedPlanningUnitsWithEntities(pusUsedIn, maxListSize, scenario.getId(), tmpVersion.getId(), dp.getId(), 0, sessionId);
                }
            }
        }
        return pusUsedIn;
    }

    public List<HspPlanningUnit> getReferencingStartedPUs(HspVersion version, List<HspPlanningUnit> pusUsedIn, int maxListSize, int sessionId) {
        hspStateMgr.verify(sessionId);
        if (version == null)
            throw new RuntimeException("Version argument cannot be null.");


        // If a list was passed in, append to it; else if no list was passed in (null) create a new one, append to it
        // as required, then return it.
        if (pusUsedIn == null)
            pusUsedIn = new ArrayList<HspPlanningUnit>();
        if ((maxListSize > 0) && (pusUsedIn.size() >= maxListSize))
            return pusUsedIn;

        Vector<HspScenario> scenarios = hspPMDB.getPMScenarios(sessionId);
        if (scenarios != null) {
            for (int loop1 = 0; loop1 < scenarios.size(); loop1++) {
                HspScenario tmpScenario = scenarios.elementAt(loop1);
                getStartedPlanningUnitsWithEntities(pusUsedIn, maxListSize, tmpScenario.getId(), version.getId(), 0, 0, sessionId);
                List<HspDPMember> dps = getDecisionPackages(sessionId);
                for (HspDPMember dp : dps) {
                    getStartedPlanningUnitsWithEntities(pusUsedIn, maxListSize, tmpScenario.getId(), version.getId(), dp.getId(), 0, sessionId);
                }
            }
        }
        return pusUsedIn;
    }

    public List<HspFXData> getReferencingFXRates(HspCurrency currency, List<HspFXData> referencingFXRates, int maxListSize, int sessionId) {
        //TODO: extend currency argument to member to accomodate currencies & time periods
        hspStateMgr.verify(sessionId);
        if (currency == null)
            throw new RuntimeException("Currency argument cannot be null.");


        // If a list was passed in, append to it; else if no list was passed in (null) create a new one, append to it
        // as required, then return it.
        if (referencingFXRates == null)
            referencingFXRates = new ArrayList<HspFXData>();
        if ((maxListSize > 0) && (referencingFXRates.size() >= maxListSize))
            return referencingFXRates;

        int currencyId = currency.getId();
        //This hash will be used for checking if the element is already added
        //It is faster to search in an int Has rather than searching for a string
        HashSet<String> tmpFxRatesUsedIn = new HashSet<String>();
        //Get Exchange Rates Usage
        Vector<HspFXData> fxDataRecords = hspCurDB.getFXData(sessionId);
        if ((fxDataRecords != null) && (fxDataRecords.size() > 0)) {
            for (int loop1 = 0; loop1 < fxDataRecords.size() && ((maxListSize < 0) || (referencingFXRates.size() < maxListSize)); loop1++) {
                HspFXData tmpFXData = fxDataRecords.elementAt(loop1);
                HspCurrency tmpCurrency = null;
                if (tmpFXData.getFromCur() == currencyId)
                    tmpCurrency = hspCurDB.getCurrency(tmpFXData.getToCur());
                if (tmpFXData.getToCur() == currencyId)
                    tmpCurrency = hspCurDB.getCurrency(tmpFXData.getFromCur());
                if (tmpCurrency != null) {
                    if (!tmpFxRatesUsedIn.contains(tmpCurrency.getCurrencyCode())) {
                        tmpFxRatesUsedIn.add(tmpCurrency.getCurrencyCode());
                        referencingFXRates.add(tmpFXData);
                    }
                }
            }
        }
        return referencingFXRates;
    }

    public List<HspMember> getReferencingMembersOfCurrency(HspCurrency currency, List<HspMember> membersReferencingCurrency, int maxListSize, int sessionId) {
        hspStateMgr.verify(sessionId);
        if (currency == null)
            throw new RuntimeException("Currency argument cannot be null.");


        // If a list was passed in, append to it; else if no list was passed in (null) create a new one, append to it
        // as required, then return it.
        if (membersReferencingCurrency == null)
            membersReferencingCurrency = new ArrayList<HspMember>();
        if ((maxListSize > 0) && (membersReferencingCurrency.size() >= maxListSize))
            return membersReferencingCurrency;

        int currencyId = currency.getId();
        //Vector entityUsedIn = new Vector();
        //Get member usage, which is just entities for now...
        Vector<HspEntity> entities = this.getEntities(sessionId);
        if (entities != null) {
            for (int loop1 = 0; loop1 < entities.size() && ((maxListSize < 0) || (membersReferencingCurrency.size() < maxListSize)); loop1++) {
                HspEntity tmpEntity = entities.elementAt(loop1);
                if ((tmpEntity != null) && (tmpEntity.getDefaultCurrency() == currencyId)) {
                    membersReferencingCurrency.add(tmpEntity);
                }
            }
        }
        return membersReferencingCurrency;
    }

    public List<HspCurrency> getReferencingTriangulatedCurrencies(HspCurrency currency, List<HspCurrency> currenciesTriangulated, int maxListSize, int sessionId) {
        // currency - see if it is used as a triangulation currency
        // return a list of currencies that use 'currency' as a triangulation currency
        hspStateMgr.verify(sessionId);
        if (currency == null)
            throw new RuntimeException("Currency argument cannot be null.");


        // If a list was passed in, append to it; else if no list was passed in (null) create a new one, append to it
        // as required, then return it.
        if (currenciesTriangulated == null)
            currenciesTriangulated = new ArrayList<HspCurrency>();
        if ((maxListSize > 0) && (currenciesTriangulated.size() >= maxListSize))
            return currenciesTriangulated;

        int currencyId = currency.getId();
        Vector<HspCurrency> currencies = hspCurDB.getSelectedCurrencies(sessionId);
        if (currencies != null) {
            for (int loop1 = 0; loop1 < currencies.size() && ((maxListSize < 0) || (currenciesTriangulated.size() < maxListSize)); loop1++) {
                HspCurrency tmpCurrency = currencies.elementAt(loop1);
                if ((tmpCurrency != null) && (tmpCurrency.getTriangulationCurrencyID() == currencyId)) {
                    currenciesTriangulated.add(tmpCurrency);
                }
            }
        }
        return currenciesTriangulated;
    }

    public List<HspForm> getReferencingForms(HspMember member, List<HspForm> formsUsedIn, int maxListSize, int sessionId) throws RuntimeException { // replaces HspFMDBImpl.getFormsReferencedToMember
        hspStateMgr.verify(sessionId);

        if (member == null)
            throw new RuntimeException("Object argument cannot be null.");


        // If a list was passed in, append to it; else if no list was passed in (null) create a new one, append to it
        // as required, then return it.
        if (formsUsedIn == null)
            formsUsedIn = new ArrayList<HspForm>();
        if ((maxListSize > 0) && (formsUsedIn.size() >= maxListSize))
            return formsUsedIn;

        // Loop through valid forms and within each form loop through all members. If the member is referenced on the
        // form, add it to the list, break, and check the next form...
        Vector<HspForm> forms = hspFMDB.getForms(sessionId);
        if (forms != null) {
            formloop:
            for (int loop1 = 0; loop1 < forms.size(); loop1++) {
                HspForm tmpForm = forms.elementAt(loop1);
                if (tmpForm != null) {
                    Vector<HspFormMember> formMembers = hspFMDB.getFormMembers(tmpForm.getId(), sessionId);
                    if (formMembers != null)
                        for (int loop2 = 0; loop2 < formMembers.size() && ((maxListSize < 0) || (formsUsedIn.size() < maxListSize)); loop2++) {
                            HspFormMember formMember = formMembers.elementAt(loop2);
                            if (formMember != null) {
                                if ((formMember.getMbrId() == member.getId()) && (formMember.getDimId() == member.getDimId())) {
                                    formsUsedIn.add(tmpForm);
                                    continue formloop;
                                }
                            }
                        }

                    /** To handle references in custom attribute display for the form */
                    Vector<HspFormAttribute> formAttrDims = ((HspFMDBImpl)hspFMDB).getFormAttributes(tmpForm.getId(), sessionId);
                    if (formAttrDims != null) {
                        for (int iFormAttrDims = 0; iFormAttrDims < formAttrDims.size(); ++iFormAttrDims) {
                            HspFormAttribute formAttr = formAttrDims.get(iFormAttrDims);
                            if ((formAttr != null) && (formAttr.getAttDimId() == member.getId())) {
                                formsUsedIn.add(tmpForm);
                                continue formloop;
                            }
                        }
                    }

                    // Look through the data validation rules associated with this form for references to the member being deleted...
                    Vector<HspDVRule> dvRules = hspFMDB.getFormDef(tmpForm.getId(), sessionId).getDataValidationRules();
                    if (dvRules != null)
                        for (HspDVRule dvRule : dvRules) {
                            HspDVCond[] conds = dvRule.getConditions();
                            for (HspDVCond cond : conds) {
                                if (cond.getCompareToValueMbrSelection() != null) {
                                    if (referencesToMemberFndInMemberSelection(cond.getCompareToValueMbrSelection(), member)) {
                                        formsUsedIn.add(tmpForm);
                                        continue formloop;
                                    }
                                }

                                if (cond.getCompareValueMbrSelection() != null) {
                                    if (referencesToMemberFndInMemberSelection(cond.getCompareValueMbrSelection(), member)) {
                                        formsUsedIn.add(tmpForm);
                                        continue formloop;
                                    }
                                }
                                HspDVPMRule[] pmRules = cond.getPMRules();
                                for (HspDVPMRule pmRule : pmRules) {
                                    if (pmRule.getApplicableSelection() != null) {
                                        if (referencesToMemberFndInMemberSelection(pmRule.getApplicableSelection(), member)) {
                                            formsUsedIn.add(tmpForm);
                                            continue formloop;
                                        }
                                    }
                                    if (pmRule.getRelatedSelection() != null) {
                                        if (referencesToMemberFndInMemberSelection(pmRule.getRelatedSelection(), member)) {
                                            formsUsedIn.add(tmpForm);
                                            continue formloop;
                                        }
                                    }
                                }
                            }
                        }
                }
            }
        }

        return formsUsedIn;
    }

    private boolean referencesToMemberFndInMemberSelection(HspMbrSelection memberSelection, HspMember member) {
        if ((memberSelection == null) || (member == null))
            return false;

        HspFormMember[] valueFormMembers = memberSelection.getMemberSelections();
        for (HspFormMember valueFormMember : valueFormMembers) {
            if (valueFormMember.getObjectType() == HspConstants.gObjType_PMDimMember) {
                HspMember pmMbr = getDimMember(memberSelection.getRequiredDimId(), valueFormMember.getIdForCache());
                if (pmMbr instanceof HspPMMember) {
                    if (member.getId() == ((HspPMMember)pmMbr).getPrimaryMemberId())
                        return true;
                }
            } else if (valueFormMember.getMbrId() == member.getId())
                return true;
        }
        return false;
    }

    public List<HspPMSVDimBinding> getReferencingPMSVDimBindings(HspScenario scenario, List<HspPMSVDimBinding> pmSVDimBindingsReferencingScenario, int maxListSize, int sessionId) {
        hspStateMgr.verify(sessionId);
        HspUtils.verifyArgumentNotNull(scenario, "scenario");
        // If a list was passed in, append to it; else if no list was passed in (null) create a new one, append to it
        // as required, then return it.
        if (pmSVDimBindingsReferencingScenario == null)
            pmSVDimBindingsReferencingScenario = new ArrayList<HspPMSVDimBinding>();
        if ((maxListSize > 0) && (pmSVDimBindingsReferencingScenario.size() >= maxListSize))
            return pmSVDimBindingsReferencingScenario;

        int scenarioId = scenario.getId();
        //Get svDIMBindings...
        Vector<HspPMSVDimBinding> bindings = hspPMDB.getPMSVDimBindings(sessionId);
        if (bindings != null) {
            for (int loop1 = 0; loop1 < bindings.size() && ((maxListSize < 0) || (pmSVDimBindingsReferencingScenario.size() < maxListSize)); loop1++) {
                HspPMSVDimBinding tmpBinding = bindings.elementAt(loop1);
                if ((tmpBinding != null) && (tmpBinding.getScenarioId() == scenarioId)) {
                    pmSVDimBindingsReferencingScenario.add(tmpBinding);
                }
            }
        }
        return pmSVDimBindingsReferencingScenario;
    }

    public List<HspPMSVDimBinding> getReferencingPMSVDimBindings(HspVersion version, List<HspPMSVDimBinding> pmSVDimBindingsReferencingVersion, int maxListSize, int sessionId) {
        hspStateMgr.verify(sessionId);
        HspUtils.verifyArgumentNotNull(version, "version");
        // If a list was passed in, append to it; else if no list was passed in (null) create a new one, append to it
        // as required, then return it.
        if (pmSVDimBindingsReferencingVersion == null)
            pmSVDimBindingsReferencingVersion = new ArrayList<HspPMSVDimBinding>();
        if ((maxListSize > 0) && (pmSVDimBindingsReferencingVersion.size() >= maxListSize))
            return pmSVDimBindingsReferencingVersion;

        int versionId = version.getId();
        //Get svDIMBindings...
        Vector<HspPMSVDimBinding> bindings = hspPMDB.getPMSVDimBindings(sessionId);
        if (bindings != null) {
            for (int loop1 = 0; loop1 < bindings.size() && ((maxListSize < 0) || (pmSVDimBindingsReferencingVersion.size() < maxListSize)); loop1++) {
                HspPMSVDimBinding tmpBinding = bindings.elementAt(loop1);
                if ((tmpBinding != null) && (tmpBinding.getVersionId() == versionId)) {
                    pmSVDimBindingsReferencingVersion.add(tmpBinding);
                }
            }
        }
        return pmSVDimBindingsReferencingVersion;
    }

    public HspCalendar getCalendar() {
        return hspCalDB.getCalendar();
    }

    public int[] getDimensionsWithEnumerationsEnabled(int planType, int sessionId) {
        // SessionId is not validated on cache hit for performance reasons.
        // The sessionId will be checked by dependant methods when cache is empty.
        synchronized (dimIdsEnabledForEnumsLock) {
            if (dimIdsEnabledForEnumsByPlanType == null)
                reloadDimIdsEnabledForEnums(sessionId);
            return dimIdsEnabledForEnumsByPlanType.get(planType);
        }
    }

    private void reloadDimIdsEnabledForEnums(int sessionId) {
        synchronized (dimIdsEnabledForEnumsLock) {
            dimIdsEnabledForEnumsByPlanType = new HashMap<Integer, int[]>();
            Vector<HspCube> cubes = getCubes(sessionId);
            for (int c = 0; c < cubes.size(); c++) {
                HspCube cube = cubes.get(c);
                if ((cube == null) || (cube.getType() == HspConstants.REPORTING_CUBE))
                    continue;
                int planType = cube.getPlanType();
                Vector<HspDimension> allDimensions = getBaseDimensions(planType, false, true, null, sessionId);
                Vector<HspReplacementDimension> replacementDimensions = getReplacementDimensions(planType, sessionId);
                if (replacementDimensions != null)
                    allDimensions.addAll(replacementDimensions);

                Vector<HspDimension> enabledDimensions = new Vector<HspDimension>();
                for (int i = 0; i < allDimensions.size(); i++) {
                    HspDimension dimension = allDimensions.get(i);
                    if (dimension != null && dimension.getEnumOrder(planType) > 0)
                        enabledDimensions.add(dimension);
                }
                // Create the dimId array, even if empty.
                int[] dimIds = new int[enabledDimensions.size()];
                if (enabledDimensions.size() > 0) {
                    // Sort the dimensions by their enumeration position.
                    Collections.sort(enabledDimensions, new HspDimEnumOrderComparator(planType));

                    // Fill in the dimId array.
                    for (int i = 0; i < dimIds.length; i++) {
                        HspDimension dimension = enabledDimensions.get(i);
                        dimIds[i] = dimension.getId();
                    }
                }
                // Add the dimIds into the map by planType.
                dimIdsEnabledForEnumsByPlanType.put(planType, dimIds);
            }
        }

    }

    public Vector<HspEnumeration> getEnumerations(int sessionId) {
        hspStateMgr.verify(sessionId);
        return resyncEnumEntriesIfNeeded(enumCache.getUnfilteredCache());
    }

    public HspEnumeration getEnumeration(int enumerationId) {
        return getEnumeration(enumerationId, -1);
    }

    public HspEnumeration getEnumeration(int enumerationId, int sessionId) {
        HspEnumeration enumeration = enumCache.getObject(enumerationId);
        if (enumeration == null)
            enumeration = generatedEnumCache.getObject(enumerationId);
        if (enumeration == null)
            enumeration = getGeneratedMetaDataEnumeration(enumerationId);

        return resyncAndFilterEnumEntriesBySecurity(enumeration, sessionId);
    }

    public HspEnumeration getGeneratedMetaDataEnumeration(int enumerationId) {
        HspEnumeration enumeration = generatedCurrencyEnumsCache.getObject(enumerationId);
        if (enumeration == null)
            enumeration = generatedSmartListsEnumsCache.getObject(enumerationId);
        if (enumeration == null)
            enumeration = generatedStaticMbrPropertiesEnumsCache.getObject(enumerationId);
        if (enumeration == null)
            enumeration = generatedFXTablesEnumsCache.getObject(enumerationId);
        if (enumeration == null)
            enumeration = generatedYearsEnumsCache.getObject(enumerationId);
        if (enumeration == null)
            enumeration = generatedSourcePlanTypeEnumsCache.getObject(enumerationId);
        if (enumeration == null)
            enumeration = generatedPeriodsEnumsCache.getObject(enumerationId);
        if (enumeration == null)
            enumeration = generatedAtttribDimMbrsEnumCache.getObject(enumerationId);

        return enumeration;
    }

    public HspEnumeration getEnumeration(String name) {
        return getEnumeration(name, -1);
    }

    public HspEnumeration getEnumeration(String name, int sessionId) {
        HspEnumeration enumeration = enumCache.getObject(name);
        if (enumeration == null)
            enumeration = generatedEnumCache.getObject(name);
        if (enumeration == null)
            enumeration = getGeneratedMetaDataEnumeration(name);

        return resyncAndFilterEnumEntriesBySecurity(enumeration, sessionId);
    }

    public HspEnumeration getGeneratedMetaDataEnumeration(String name) {
        HspEnumeration enumeration = generatedCurrencyEnumsCache.getObject(name);
        if (enumeration == null)
            enumeration = generatedSmartListsEnumsCache.getObject(name);
        if (enumeration == null)
            enumeration = generatedStaticMbrPropertiesEnumsCache.getObject(name);
        if (enumeration == null)
            enumeration = generatedFXTablesEnumsCache.getObject(name);
        if (enumeration == null)
            enumeration = generatedYearsEnumsCache.getObject(name);
        if (enumeration == null)
            enumeration = generatedSourcePlanTypeEnumsCache.getObject(name);
        if (enumeration == null)
            enumeration = generatedPeriodsEnumsCache.getObject(name);
        if (enumeration == null)
            enumeration = generatedAtttribDimMbrsEnumCache.getObject(name);

        return enumeration;
    }

    public HspEnumeration filterEnumEntriesByValidComboRules(HspEnumeration enumeration, HspFormCell[] currCtx, int sessionId) {
        if (currCtx == null)
            return enumeration;

        if (enumeration != null && enumeration.isVirtual()) {
            //            HspMember mbrDrivingEnum = getMemberById(enumeration.getPlanType(), enumeration.getMbrSelId());
            //            if (mbrDrivingEnum != null) {
            // Get all allowed mbrs
            Vector<HspFormMember> mbrSelItemList = new Vector<HspFormMember>(Arrays.asList(enumeration.getMemberSelection().getMemberSelections()));
            Vector<HspFormMember> attribMembers = hspJS.getFMDB(sessionId).getAndRemoveAttributeMembers(mbrSelItemList);
            HspMembersByAttributesFilterer attributesFilterer = new HspMembersByAttributesFilterer(attribMembers, null, false, hspJS, sessionId);
            Vector<HspFormCell> members = HspRTPUtils.evaluateMembersVectorAndSubstitutionVars(mbrSelItemList, -1, hspJS, hspJS.getMyUserId(sessionId), false, sessionId);
            attributesFilterer.filterFormCells(members);

            if (HspUtils.isNullOrEmpty(members)) {
                enumeration = (HspEnumeration)enumeration.cloneForUpdate();
                enumeration.setEntries(new HspEnumeration.Entry[0]);
                return enumeration;
            }

            HspFormCell[] filteredCells = hspFMDB.getValidMbrs(enumeration.getDimId(), members.toArray(new HspFormCell[members.size()]), currCtx, enumeration.getPlanType(), sessionId);
            //Get the intersection of members that exist in both filteredCells and the enumeration entries...
            List<HspEnumeration.Entry> filteredEntries = new ArrayList<HspEnumeration.Entry>();
            for (HspEnumeration.Entry entry : enumeration.getEntries()) {
                for (HspFormCell cell : filteredCells) {
                    if (cell.getMbrId() == entry.getEntryId()) {
                        filteredEntries.add(entry);
                        break;
                    }
                }
            }

            enumeration = (HspEnumeration)enumeration.cloneForUpdate();
            enumeration.setEntries(filteredEntries.toArray(new HspEnumeration.Entry[filteredEntries.size()]));
        }
        //        }
        return enumeration;
    }

    private Vector<HspEnumeration> resyncEnumEntriesIfNeeded(Vector<HspEnumeration> enumerations) {
        if (enumerations == null || enumerations.size() == 0)
            return enumerations;

        Vector<HspEnumeration> result = new Vector<HspEnumeration>(enumerations.size());
        for (HspEnumeration enumeration : enumerations) {
            result.add(resyncEnumEntriesIfNeeded(enumeration));
        }
        return result;
    }

    private HspEnumeration resyncEnumEntriesIfNeeded(HspEnumeration enumeration) {
        if (enumeration == null || !enumeration.isVirtual())
            return enumeration;
        // If the member driven smart list is stale, update it first
        Long dimMembersImpactedTime = dimMembersImpactedTimeMap.get(enumeration.getDimId());
        if (dimMembersImpactedTime != null) {
            synchronized (enumeration) {
                Long lastRebuiltTime = memberDrivenSmartListRebuiltTimeMap.get(enumeration.getEnumerationId());
                if (lastRebuiltTime == null || lastRebuiltTime < dimMembersImpactedTime) {
                    lastRebuiltTime = System.currentTimeMillis();
                    notifier.fireChangeEvent(new HspChangeEvent(HspEnumeration.class, enumeration, HspChangeEvent.ACTION_MODIFIED));
                    memberDrivenSmartListRebuiltTimeMap.put(enumeration.getEnumerationId(), lastRebuiltTime);
                    HspEnumeration updatedEnumeration = enumCache.getObject(enumeration.getEnumerationId());
                    if (updatedEnumeration != null)
                        enumeration = updatedEnumeration;
                }
            }
        }
        return enumeration;
    }

    private HspEnumeration resyncAndFilterEnumEntriesBySecurity(HspEnumeration enumeration, int sessionId) {
        enumeration = resyncEnumEntriesIfNeeded(enumeration);
        if (sessionId < 0)
            return enumeration;
        HspEnumeration.Entry[] entries = enumeration.getEntries();
        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));

        if (user.getUserRole() != HspConstants.USER_BUDGET_ADMIN && enumeration.isVirtual()) {
            if (isDimensionSecured(enumeration.getDimId(), sessionId)) {
                //if (doesMemberDeriveSL(getMemberById(enumeration.getPlanType(), enumeration.getMbrSelId()), enumeration.getPlanType())) {
                List<HspEnumeration.Entry> filteredList = new ArrayList<HspEnumeration.Entry>();
                for (HspEnumeration.Entry entry : entries) {
                    HspMember entityMbr = getMemberById(entry.getEntryId());
                    int access = hspSecDB.getAccess(hspSecDB.getUser(hspJS.getMyUserId(sessionId)), entityMbr);
                    if ((access & HspConstants.ACCESS_READ) == HspConstants.ACCESS_READ || (access & HspConstants.ACCESS_WRITE) == HspConstants.ACCESS_WRITE)
                        filteredList.add(entry);
                }
                enumeration = (HspEnumeration)enumeration.cloneForUpdate();
                enumeration.setEntries(filteredList.toArray(new HspEnumeration.Entry[filteredList.size()]));
                //}
            }
        }
        return enumeration;
    }

    //    public boolean doesMemberDeriveSL(HspMember member, int planType){
    //        Vector<HspEnumeration> enums =  enumCache.getObjects(enumMbrIdKeyDef, enumMbrIdKeyDef.createKeyFromMemberId(member.getId(), planType));
    //        return !HspUtils.isNullOrEmpty(enums) ? true : false;
    //    }

    //    public HspEnumeration getMemberDerivedSL(HspMember member, int planType) {
    //       Vector<HspEnumeration> enums =  enumCache.getObjects(enumMbrIdKeyDef, enumMbrIdKeyDef.createKeyFromMemberId(member.getId(), planType));
    //       return !HspUtils.isNullOrEmpty(enums) ? enums.get(0) : null;
    //    }

    //    public Set<HspMember> getMbrsDrivingSmartLists(int planType) {
    //        Set<HspMember> mbrsDrivingSLs = new HashSet<HspMember>();
    //        Vector<HspEnumeration> enums = enumCache.getUnfilteredCache();
    //        for(HspEnumeration enumeration : enums) {
    //            if(enumeration.getMbrSelId() > 0 && enumeration.getPlanType() > 0 && (enumeration.getPlanType() & planType) == enumeration.getPlanType()) {
    //                HspMember mbr = getMemberById(enumeration.getPlanType(), enumeration.getMbrSelId());
    //                if(mbr != null)
    //                    mbrsDrivingSLs.add(mbr);
    //            }
    //        }
    //        return mbrsDrivingSLs;
    //    }

    public synchronized void addEnumeration(HspEnumeration enumeration, int sessionId) throws Exception {
        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        addEnumeration(enumeration, actionSet, sessionId);
        actionSet.doActions();
    }

    public synchronized void addEnumeration(HspEnumeration enumeration, HspActionSet actionSet, int sessionId) throws Exception {
        hspStateMgr.verify(sessionId);
        enumeration.validate();
        HspEnumeration existingEnumeration = getEnumeration(enumeration.getName());
        if (existingEnumeration != null)
            throw new DuplicateObjectException(enumeration.getName());
        HspEnumerationAction action = new HspEnumerationAction();
        actionSet.addAction(action, HspActionSet.ADD, enumeration);
    }

    public synchronized void updateEnumeration(HspEnumeration enumeration, int sessionId) throws Exception {
        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        updateEnumeration(enumeration, actionSet, sessionId);
        actionSet.doActions();
    }

    public synchronized void updateEnumeration(HspEnumeration enumeration, HspActionSet actionSet, int sessionId) throws Exception {
        hspStateMgr.verify(sessionId);
        enumeration.validate();

        HspEnumeration existingEnumeration = getEnumeration(enumeration.getName());
        if (existingEnumeration != null && existingEnumeration.getEnumerationId() != enumeration.getEnumerationId())
            throw new DuplicateObjectException(enumeration.getName());
        HspEnumerationAction action = new HspEnumerationAction();
        actionSet.addAction(action, HspActionSet.UPDATE, enumeration);
    }

    public synchronized void deleteEnumeration(HspEnumeration enumeration, int sessionId) throws Exception {
        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        deleteEnumeration(enumeration, actionSet, sessionId);
        actionSet.doActions();
    }

    public synchronized void deleteEnumeration(HspEnumeration enumeration, HspActionSet actionSet, int sessionId) throws Exception {
        hspStateMgr.verify(sessionId);
        int enumerationId = enumeration.getEnumerationId();
        Vector<HspDimension> dimensions = getBaseDimensions(sessionId);
        for (int i = 0; i < dimensions.size(); i++) {
            HspDimension dimension = dimensions.get(i);
            HspAction action = createAction(dimension.getObjectType());
            Vector<HspMember> members = getDimMembers(dimension.getId(), false, sessionId);
            for (int j = 0; j < members.size(); j++) {
                HspMember member = members.get(j);
                if (member.getEnumerationId() == enumerationId) {
                    member = (HspMember)member.cloneForUpdate();
                    member.setEnumerationId(0);
                    actionSet.addAction(action, HspActionSet.UPDATE, member);
                }
            }
        }

        // Delete references to this enumeration from form members.
        HspAction frmMbrAction = new HspUpdateFormMemberCustomAction();
        actionSet.addAction(frmMbrAction, HspActionSet.CUSTOM, enumeration);

        //Delete references to this enumeration from CubeLinks
        hspCubeLinkDB.deleteSmartLinkUsageFromCubeLinks(enumeration, actionSet, sessionId);

        HspEnumerationAction action = new HspEnumerationAction();
        actionSet.addAction(action, HspActionSet.DELETE, enumeration);
    }

    public Vector<HspUDA> getUDAs(int sessionId) {
        hspStateMgr.verify(sessionId);
        return udaCache.getUnfilteredCache();
    }

    public Vector<HspUDABinding> getAllUDABindings(int sessionId) {
        hspStateMgr.verify(sessionId);
        return udaBindingCache.getUnfilteredCache();
    }

    public Vector<HspUDA> getUDAs(int dimId, int sessionId) {
        hspStateMgr.verify(sessionId);
        return udaCache.getObjects(udaDimIdKeyDef, udaDimIdKeyDef.createKeyFromDimId(dimId));
    }

    public HspUDA getUDA(int udaId) {
        return udaCache.getObject(udaKeyDef, udaKeyDef.createKeyFromUDAId(udaId));
    }

    public HspUDA getUDA(int dimId, String udaValue) {
        return udaCache.getObject(udaDimIdUDAValueKeyDef, udaDimIdUDAValueKeyDef.createKeyFromDimIdUDAValue(dimId, udaValue));
    }

    public synchronized void addUDA(HspUDA uda, int sessionId) throws Exception {
        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        addUDA(uda, actionSet, sessionId);
        actionSet.doActions();
    }

    public synchronized void addUDA(HspUDA uda, HspActionSet actionSet, int sessionId) throws Exception {
        hspStateMgr.verify(sessionId);
        HspUtils.verifyArgumentNotNull(uda, "User defined attribute");
        HspUDA existingUDA = getUDA(uda.getDimId(), uda.getName());
        if (existingUDA != null)
            throw new DuplicateObjectException(uda.getName());
        validateAttributeName(uda.getName());
        HspUDAAction action = new HspUDAAction();
        actionSet.addAction(action, HspActionSet.ADD, uda);

        // mark outline as changed so cube refresh updates UDA's and bindings
        HspSystemCfg hspSystemConfig = hspJS.getSystemCfg();
        HspSystemCfg systemConfig = (HspSystemCfg)hspSystemConfig.cloneForUpdate();
        systemConfig.setOtlchgd(true); // that screwy method name means 'set outline changed'
        actionSet.addAction(new HspSystemCfgAction(), HspActionSet.UPDATE, systemConfig);
    }

    public synchronized void updateUDA(HspUDA uda, int sessionId) throws Exception {
        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        updateUDA(uda, actionSet, sessionId);
        actionSet.doActions();
    }

    public synchronized void updateUDABinding(HspUDABinding udaBinding, int sessionId) throws Exception {
        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        HspUDABindingAction action = new HspUDABindingAction();
        actionSet.addAction(action, HspActionSet.UPDATE, udaBinding);
        actionSet.doActions();
    }

    public synchronized void updateUDA(HspUDA uda, HspActionSet actionSet, int sessionId) throws Exception {
        hspStateMgr.verify(sessionId);
        HspUtils.verifyArgumentNotNull(uda, "User defined attribute");
        HspUDA existingUDA = getUDA(uda.getDimId(), uda.getName());
        if (existingUDA != null && existingUDA.getId() != uda.getId())
            throw new DuplicateObjectException(uda.getName());
        validateAttributeName(uda.getName());
        HspUDAAction action = new HspUDAAction();
        actionSet.addAction(action, HspActionSet.UPDATE, uda);

        // mark outline as changed so cube refresh updates UDA's and bindings
        HspSystemCfg hspSystemConfig = hspJS.getSystemCfg();
        HspSystemCfg systemConfig = (HspSystemCfg)hspSystemConfig.cloneForUpdate();
        systemConfig.setOtlchgd(true); // that screwy method name means 'set outline changed'
        actionSet.addAction(new HspSystemCfgAction(), HspActionSet.UPDATE, systemConfig);
    }

    public synchronized void deleteUDA(HspUDA uda, int sessionId) throws Exception {
        HspUDA fetchedUDA = getUDA(uda.getDimId(), uda.getUdaValue());
        if (fetchedUDA == null)
            throw new RuntimeException("Unable to delete UDA \"" + uda.getUdaValue() + "\" as it does not exist.");


        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        deleteUDA(uda, actionSet, sessionId);
        actionSet.doActions();
    }

    public synchronized void deleteUDA(HspUDA uda, HspActionSet actionSet, int sessionId) throws Exception {
        HspUDA fetchedUDA = getUDA(uda.getDimId(), uda.getUdaValue());
        if (fetchedUDA == null)
            throw new RuntimeException("Unable to delete UDA \"" + uda.getUdaValue() + "\" as it does not exist.");


        hspStateMgr.verify(sessionId);
        HspUDAAction action = new HspUDAAction();
        actionSet.addAction(action, HspActionSet.DELETE, uda);

        // mark outline as changed so cube refresh updates UDA's and bindings
        HspSystemCfg hspSystemConfig = hspJS.getSystemCfg();
        HspSystemCfg systemConfig = (HspSystemCfg)hspSystemConfig.cloneForUpdate();
        systemConfig.setOtlchgd(true); // that screwy method name means 'set outline changed'
        actionSet.addAction(new HspSystemCfgAction(), HspActionSet.UPDATE, systemConfig);
    }

    public synchronized void deleteUnsuedUDAs(int sessionId) throws Exception {
        hspStateMgr.verify(sessionId);
        Vector<HspUDABinding> bindings = udaBindingCache.getUnfilteredCache();
        Set<Integer> inUseUDAIdSet = new HashSet<Integer>();
        if (bindings != null) {
            for (int i = 0; i < bindings.size(); i++) {
                HspUDABinding binding = bindings.get(i);
                Integer udaIdKey = binding.getUDAId();
                inUseUDAIdSet.add(udaIdKey);
            }
        }

        Vector<HspUDA> udas = udaCache.getUnfilteredCache();
        if (udas != null) {
            for (int i = 0; i < udas.size(); i++) {
                HspUDA uda = udas.get(i);
                Integer udaIdKey = uda.getId();
                if (!inUseUDAIdSet.contains(udaIdKey)) {
                    deleteUDA(uda, sessionId);
                    HspLogger.trace("Deleting unused UDA: " + uda);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     * @param memberId {@inheritDoc}
     * @return {@inheritDoc}
     */
    public Vector<HspUDABinding> getUDABindings(int memberId) {
        return udaBindingCache.getObjects(udaBindingsForMemberKeyDef, udaBindingsForMemberKeyDef.createKeyFromMemberId(memberId));
    }

    /**
     * {@inheritDoc}
     * @param udaId {@inheritDoc}
     * @return {@inheritDoc}
     */
    public Vector<HspUDABinding> getUDABindingsForUDA(int udaId) {
        return udaBindingCache.getObjects(udaBindingsForUDAKeyDef, udaBindingsForUDAKeyDef.createKeyFromUDAId(udaId));
    }

    public HspUDABinding getUDABinding(int memberId, int udaId) {
        return udaBindingCache.getObject(udaBindingKeyDef, udaBindingKeyDef.createKeyFromMemberIdUDAId(memberId, udaId));
    }

    public Vector<HspUDA> getUDAsBoundToMember(int memberId) {
        Vector<HspUDA> result;
        Vector<HspUDABinding> bindings = getUDABindings(memberId);
        if (bindings == null)
            result = new Vector<HspUDA>(0);
        else {
            int numBindings = bindings.size();
            result = new Vector<HspUDA>(numBindings);
            for (int i = 0; i < numBindings; i++) {
                HspUDABinding binding = bindings.get(i);
                if (binding != null) {
                    HspUDA uda = getUDA(binding.getUDAId());
                    if (uda == null)
                        HspLogger.trace("Ignoring invalid udaId " + binding.getUDAId() + " bound to member " + binding.getMemberId() + ".");
                    else
                        result.add(uda);
                }
            }
        }
        return result;
    }

    private Vector<HspMember> getMembersBoundToUDA(int udaId, Predicate predicate, boolean includeIndirectSharedMembers, int sessionId) {
        hspStateMgr.verify(sessionId);
        Vector<HspMember> result;
        HspUDA uda = getUDA(udaId);
        Vector<HspUDABinding> bindings = getUDABindingsForUDA(udaId);
        if (uda == null || bindings == null)
            result = new Vector<HspMember>(0);
        else {
            result = new Vector<HspMember>(bindings.size());
            int dimId = uda.getDimId();
            GenericCache<? extends HspMember> memberCache = getMembersCache(dimId);
            for (HspUDABinding binding : bindings) {
                if (binding != null) {
                    HspMember member = getDimMember(dimId, binding.getMemberId());
                    if (member == null)
                        HspLogger.trace("Ignoring invalid memberId " + binding.getMemberId() + " bound to UDA " + udaId + ".");
                    else {
                        if (predicate == null || predicate.evaluate(member))
                            result.add(member);
                        if (includeIndirectSharedMembers) {
                            List<? extends HspMember> sharedMembers = memberCache.getObjects(memberBaseMemberIdKeyDef, memberBaseMemberIdKeyDef.createKeyFromBaseMemberId(member.getId()));
                            if (sharedMembers != null) {
                                for (HspMember sharedMember : sharedMembers) {
                                    if (predicate == null || predicate.evaluate(sharedMember))
                                        result.add(sharedMember);
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }


    public Vector<HspMember> getMembersBoundToUDA(int udaId, boolean includeIndirectSharedMembers, int sessionId) {
        return getMembersBoundToUDA(udaId, null, includeIndirectSharedMembers, sessionId);
    }

    public Vector<HspMember> getMembersBoundToUDA(int udaId, int planTypes, boolean includeIndirectSharedMembers, int sessionId) {
        return getMembersBoundToUDA(udaId, HspPredicateUtils.usedInPredicate(planTypes), includeIndirectSharedMembers, sessionId);
    }

    private HspMember getMemberByName(int planTypes, String name, boolean useOldName) {
        HspMember member = null;
        if (name != null) {
            Vector<HspDimension> dimensions = getAllDimensions(planTypes, internalSessionId);
            if (dimensions == null || dimensions.size() == 0) {
                HspCube essCube = getEssbaseCubeByPlanType(planTypes);
                if (essCube != null) {
                    dimensions = getReportingCubeDimensions(getEssbaseServer(essCube.getEssbaseServerId(), internalSessionId), essCube.getAppName(), essCube.getCubeName(), internalSessionId);
                }
            }
            // Check dimensions and members;
            if (dimensions != null) {
                for (Iterator<HspDimension> it = dimensions.iterator(); it.hasNext(); ) {
                    HspDimension dim = it.next();
                    if (!useOldName)
                        member = getDimMember(dim.getId(), name);
                    else
                        member = getDimMember(dim.getId(), HspObjectOldNameKeyDef.HSP_OBJECT_OLD_NAME_KEY, name);
                    if (member != null)
                        break;
                }
            }
            if (member == null)
                member = getDimMember(HspConstants.kDimensionMetric, name);
            if (member == null) {
                Vector<HspReplacementDimension> replacementDims = getReplacementDimensions(planTypes, internalSessionId);
                if (replacementDims != null) {
                    for (HspReplacementDimension replacementDim : replacementDims) {
                        member = getDimMember(replacementDim.getId(), name);
                        if (member != null)
                            break;
                    }
                }
            }
        }

        return member;
    }

    public HspMember getMemberByName(String name) {
        return getMemberByName(HspConstants.PLAN_TYPE_ALL, name);
    }

    public HspMember getMemberByName(int planTypes, String name) {
        return getMemberByName(planTypes, name, false);
    }

    public HspMember getMemberByEssbaseName(String name) {
        return getMemberByName(HspConstants.PLAN_TYPE_ALL, name, true);
    }

    public HspMember getMemberById(int planTypes, int memId) {
        HspMember member = null;
        if (memId != 0) {
            Vector<HspDimension> dimensions = getAllDimensions(planTypes, internalSessionId);
            //Check dimensions and members;
            if (dimensions != null) {
                for (Iterator<HspDimension> it = dimensions.iterator(); it.hasNext(); ) {
                    HspDimension dim = it.next();
                    member = getDimMember(dim.getId(), memId);
                    if (member != null)
                        break;
                }
            }
            //TODO LM bug #24911371 if member not found check metric and replacement dimensions
            if (member == null)
                member = getDimMember(HspConstants.kDimensionMetric, memId);
            if (member == null) {
                Vector<HspReplacementDimension> replDims = getReplacementDimensions(planTypes, internalSessionId);
                if (replDims != null) {
                    for (Iterator<HspReplacementDimension> it = replDims.iterator(); it.hasNext(); ) {
                        HspReplacementDimension dim = it.next();
                        member = getDimMember(dim.getId(), memId);
                        if (member != null)
                            break;
                    }
                }
            }
        }
        return member;
    }

    public HspMember getMemberById(int memId) {
        return getMemberById(HspConstants.PLAN_TYPE_ALL, memId);
    }

    private void validateDelete(HspMember member, int sessionId) throws HspRuntimeException {
        hspStateMgr.verify(sessionId);
        if (member != null) {
            Properties p = new Properties();

            // Check to see if this member in non-removable
            if (!member.isRemovable()) {
                p.put("DIMENSION_MEMBER_NAME", member.getObjectName());
                throw new HspRuntimeException("MSG_CANNOT_DELETE_NONREMOVABLE_MEMBER", p);


            }

            //Check for Form reference
            ArrayList list = (ArrayList)this.getReferencingForms(member, null, 1, sessionId);
            if (list != null && list.size() > 0) {
                if (member instanceof HspAttributeMember) {
                    p.put("DIMENSION_MEMBER_NAME", member.getObjectName());
                    throw new HspRuntimeException("MSG_ATTRMEMBER_FORM_REFERENCE", p);
                } else {
                    p.put("DIMENSION_MEMBER_NAME", member.getObjectName());
                    throw new HspRuntimeException("MSG_MEMBER_FORM_REFERENCE", p);
                }
            }

            Vector<HspEnumeration> enumsList = getEnumerations(sessionId);
            if (enumsList != null) {
                for (HspEnumeration enumeration : enumsList) {
                    if (!enumeration.isVirtual())
                        continue;
                    HspMbrSelection mbrSel = enumeration.getMemberSelection();
                    if (mbrSel != null) {
                        for (HspFormMember mbr : mbrSel.getMemberSelections()) {
                            if (mbr.getMbrId() == member.getId()) {
                                p.put("DIMENSION_MEMBER_NAME", member.getObjectName());
                                throw new HspRuntimeException("MSG_MEMBER_ENUM_REFERENCE", p);
                            }
                        }
                    }
                }
            }

            // Look through the valid interscetion rules for references to the member being deleted...
            Vector<HspVCRule> vcRules = hspFMDB.getVCRulesForEditing(sessionId);
            for (HspVCRule vcRule : vcRules) {
                HspVCSubRule[] subRules = vcRule.getSubRules();
                if (!HspUtils.isNullOrEmpty(subRules)) {
                    for (HspVCSubRule subRule : subRules) {
                        if (subRule.getMemberSelection() != null) {
                            if (referencesToMemberFndInMemberSelection(subRule.getMemberSelection(), member)) {
                                p.put("DIMENSION_MEMBER_NAME", member.getObjectName());
                                p.put("VC_RULE_NAME", vcRule.getName());
                                throw new HspRuntimeException("MSG_MEMBER_VCRULE_REFERENCE", p);
                            }
                        }
                        if (subRule.getExcludeMemberSelection() != null) {
                            if (referencesToMemberFndInMemberSelection(subRule.getExcludeMemberSelection(), member)) {
                                p.put("DIMENSION_MEMBER_NAME", member.getObjectName());
                                p.put("VC_RULE_NAME", vcRule.getName());
                                throw new HspRuntimeException("MSG_MEMBER_VCRULE_REFERENCE", p);
                            }
                        }
                    }
                }
            }

            List<HspCubeLink> cubeLinksUsed = hspJS.getCubeLinkDB(sessionId).getReferencingCubeLinks(member, null, 1, sessionId);

            if (cubeLinksUsed.size() > 0) {
                p.put("DIMENSION_MEMBER_NAME", member.getObjectName());
                throw new HspRuntimeException("MSG_MEMBER_DATA_MAP_REFERENCE", p);
            }

            isValidForDelete(member, sessionId);
        }
    }

    private boolean isValidForDelete(HspMember member, int sessionId) {
        if (member == null)
            return (false);
        Properties p = new Properties();
        switch (member.getObjectType()) {
            //Checks for Currency - triangulation and Entitiy reference
        case HspConstants.gObjType_Currency:
        case HspConstants.gObjType_CurrencyMember:
            {
                HspCurrency currency = (HspCurrency)member;
                p.put("CURRENCY_CODE", currency.getCurrencyCode());
                if (hspCurDB.isTriangulated(currency, sessionId))
                    throw new HspRuntimeException("MSG_CANT_DELETE_CURRENCIES", p);


                //if (hspCurDB.isReferencedByEntities(currency,sessionId))
                ArrayList list = (ArrayList)getReferencingMembersOfCurrency(currency, null, 1, sessionId);
                if (list != null && list.size() > 0)
                    throw new HspRuntimeException("MSG_CANT_DELETE_CURRENCIES", p);


                break;
            }
            //Checks for Scenario - reference to Planning Units that are started and PMSVDim bindings and references in Decision Package dimensions
        case HspConstants.gObjType_Scenario:
            {
                p.put("SCENARIO_NAME", member.getObjectName());
                ArrayList list = (ArrayList)getReferencingStartedPUs(member, null, 1, sessionId);
                if (list != null && list.size() > 0)
                    throw new HspRuntimeException("MSG_CANT_DELETE_SCE_USED_IN_PU", p);


                HspScenario scenario = (HspScenario)member;
                list = (ArrayList)getReferencingPMSVDimBindings(scenario, null, 1, sessionId);
                if (list != null && list.size() > 0)
                    throw new HspRuntimeException("MSG_CANT_DELETE_SCE_USED_IN_PUH", p);


                Vector<HspDPDimension> dpDims = getDPDimensions(scenario.getIdForCache(), sessionId);
                if (dpDims != null && dpDims.size() > 0)
                    throw new HspRuntimeException("MSG_CANT_DELETE_SCE_USED_IN_DPMBR", p);


                break;
            }
            //Checks for Version - reference to Planning Units that are started and PMSVDim bindings and references in Decision Package members
        case HspConstants.gObjType_Version:
            {
                p.put("VERSION_NAME", member.getObjectName());
                ArrayList list = (ArrayList)getReferencingStartedPUs(member, null, 1, sessionId);
                if (list != null && list.size() > 0)
                    throw new HspRuntimeException("MSG_CANT_DELETE_VER_USED_IN_PU", p);


                HspVersion version = (HspVersion)member;
                list = (ArrayList)getReferencingPMSVDimBindings(version, null, 1, sessionId);
                if (list != null && list.size() > 0)
                    throw new HspRuntimeException("MSG_CANT_DELETE_VER_USED_IN_PUH", p);
                List<HspDPMember> dps = getDecisionPackages(sessionId);
                for (HspDPMember dp : dps) {
                    if (dp.getVersionId() == version.getIdForCache())
                        throw new HspRuntimeException("MSG_CANT_DELETE_VER_USED_IN_DPMBR", p);


                }
                break;
            }
            //Checks for Entity - references in Decision Package members
        case HspConstants.gObjType_Entity:
            {
                p.put("ENTITY_NAME", member.getObjectName());
                HspEntity entity = (HspEntity)member;
                List<HspDPMember> dps = getDecisionPackages(sessionId);
                for (HspDPMember dp : dps) {
                    if (dp.getEntityId() == entity.getIdForCache())
                        throw new HspRuntimeException("MSG_CANT_DELETE_ENTITIES", p);


                    ArrayList<Integer> brSharingEntitiesList = new ArrayList<Integer>(Arrays.asList(ArrayUtils.toObject(dp.getSharingEntityIds())));
                    if (brSharingEntitiesList.contains(entity.getIdForCache()))
                        throw new HspRuntimeException("MSG_CANT_DELETE_ENTITIES", p);


                }
                break;
            }
        default:
        }
        return (true);
    }

    private boolean isReferencedByPlanningUnits(HspMember o, int sessionId) {
        ArrayList list = (ArrayList)getReferencingStartedPUs(o, null, 1, sessionId);
        if (list != null && list.size() > 0)
            return true;

        return false;
    }

    private boolean isFlatDimensionMember(HspMember member) {
        if (member == null)
            throw new RuntimeException("Member argument is null.");


        switch (member.getObjectType()) {
            // The following made hierarchical for 9.3.1
            //case HspConstants.gObjType_AttributeMember:
            //case HspConstants.gObjType_Scenario:
            //case HspConstants.gObjType_Version:
        case HspConstants.gObjType_Currency:
            return hspJS.getSystemCfg().isMultiCurrency();
        case HspConstants.gObjType_Year:
            return true;
        default:
            return false;
        }
    }

    private void validateFlatDimensionMembersParent(HspMember member) throws HspRuntimeException {
        if (member == null)
            throw new RuntimeException("Member argument is null.");


        if (!isFlatDimensionMember(member))
            return;
        //do not check for Year members for Parent as Root if All Years exist because all FYs parent
        //would be now "All Years' member.
        if (member.getObjectType() == HspConstants.gObjType_Year && hspCalDB.hasAllYearsParent())
            return;

        if (member.getObjectType() == HspConstants.gObjType_SimpleCurrency)
            return;
        /*
        HspDimension dimension = getDimRoot(member.getDimId());
        if (dimension == null)
            throw new RuntimeException("Invalid Dimension Id for member: "+member.getName());
*/
        if (!(member.getParentId() == member.getDimId() || member.getId() == member.getDimId())) {
            Properties p = new Properties();
            p.put("MEMBER_NAME", member.getName());
            throw new HspRuntimeException("MSG_PARENTID_NOT_ROOT_FOR_FLAT_DIM_MEMBER", p);


        }
    }

    public void validateMemberFormula(HspMember member, String formula, boolean returnOldName, int sessionId) throws Exception {
        //todo: finishing implementing this method
        if (member == null)
            throw new IllegalArgumentException("Member is null.");

        if (formula != null && formula.length() > 0) {
            boolean doDefaultCheck = false;
            HspMemberPTProps[] props = member.getMemberPTPropsToBeSaved();

            if (props == null || props.length == 0 || formula.equalsIgnoreCase(member.getFormula(0)))
                doDefaultCheck = true;

            if (doDefaultCheck) {
                // The formula should only be verifed in the source plan type for Acounts.
                // For all other members, verify the formula in all used cubes.
                Vector<HspCube> usedCubes;
                if (member.getDimId() == HspConstants.kDimensionAccount) {
                    usedCubes = new Vector<HspCube>(1);
                    HspAccount account = (HspAccount)member;
                    HspCube cube = getCubeByPlanType(account.getSrcPlanType());
                    if (cube != null && cube.getType() != HspConstants.ASO_CUBE)
                        usedCubes.add(cube);
                } else
                    usedCubes = getCubesByUsedIn(member.getUsedIn(), true, sessionId);

                if (usedCubes == null || usedCubes.size() == 0)
                    throw new HspRuntimeException("MSG_MEMBER_MUST_BELONG_TO_CUBE_TO_VALIDATE_FORMULA");


                MemberFormulaExpressionTranslator expresionTranslator = new DefaultMemberFormulaExpressionTranslator(hspJS, sessionId, member, true, returnOldName);
                MemberFormulaTranslator translator = new MemberFormulaTranslator(expresionTranslator);
                try {
                    formula = translator.translate(formula);
                } catch (Exception ex) {
                    logger.fine("Exception occurred while translating formula...using untranslated formula");
                }
                for (int i = 0; i < usedCubes.size(); i++) {
                    HspCube cube = usedCubes.get(i);
                    try {

                        hspOLAP.EssVerifyFormula(formula, hspJS.getAppName(), cube.getCubeName());
                    } catch (Exception e) {
                        throw new HspRuntimeException("MSG_MEMBER_FORMULA_VALIDATION_FAILED", e);

                    }
                }
            } else {
                // find the plan type specific formula and only validate against this plan type
                for (int i = 0; i < props.length; i++) {
                    HspMemberPTProps prop = props[i];

                    if (prop.getPlanType() > 0 && formula.equalsIgnoreCase(prop.getFormula())) {
                        HspCube cube = getCubeByPlanType(prop.getPlanType());
                        String appName = hspJS.getAppName();

                        if (cube != null && cube.getType() == HspConstants.ASO_CUBE)
                            appName = cube.getAppName();
                        try {
                            MemberFormulaExpressionTranslator expresionTranslator = new DefaultMemberFormulaExpressionTranslator(hspJS, sessionId, member, true, returnOldName);
                            MemberFormulaTranslator translator = new MemberFormulaTranslator(expresionTranslator);
                            try {
                                formula = translator.translate(formula);
                            } catch (Exception ex) {
                                logger.fine("Exception occurred while translating formula...using untranslated formula");
                            }

                            hspOLAP.EssVerifyFormula(formula, appName, cube.getCubeName());
                        } catch (Exception e) {
                            throw new HspRuntimeException("MSG_MEMBER_FORMULA_VALIDATION_FAILED", e);
                        }
                    }
                }
            }
        }
    }

    public void validateEnumerationId(HspMember member) {
        if (member.getEnumerationId() > 0) {
            HspEnumeration enumeration = this.getEnumeration(member.getEnumerationId());
            if (enumeration == null)
                throw new RuntimeException("Invalid enumeration id specified.");


        }
    }

    public Vector syncMemberFormulas(int sessionId, String cuttOffString, boolean bDisplayErrors) throws Exception {

        //   cuttoff string is used to filter out member Formula errors fro the rest of the messages
        Vector<HspCube> cubes = getCubes(sessionId);
        Vector vErrorsforallCubes = new Vector();

        MemberFormulaExpressionTranslator expresionTranslator = new DefaultMemberFormulaExpressionTranslator(hspJS, sessionId, null, true, false);
        MemberFormulaTranslator translator = new MemberFormulaTranslator(expresionTranslator);

        for (int n = 0; n < cubes.size(); n++) {
            HspCube hspCube = cubes.elementAt(n);
            Vector<HspDimension> dims = getBaseDimensions(hspCube.getPlanType(), sessionId);
            Vector[] members = new Vector[dims.size()];
            for (int nDimIndex = 0; nDimIndex < dims.size(); nDimIndex++) {
                HspDimension hspDimension = dims.elementAt(nDimIndex);
                members[nDimIndex] = getDimMembers(hspDimension.dimId, false, sessionId);
            }
            Vector vErrors = hspOLAP.setMbrFormulaForMembers(translator, hspJS.getAppName(), hspCube, members);
            if (vErrors != null && bDisplayErrors && vErrors.size() != 0) {

                String s = cuttOffString + "There are the following member formula errors in cube %CUBE% : ";
                String sDescrMessage = HspUtils.replace(s, "%CUBE%", hspCube.getCubeName());
                logger.finer(sDescrMessage);
                for (int nErrorIndex = 0; nErrorIndex < vErrors.size(); nErrorIndex++)
                    logger.finer("Error while syncMemberFormulas: {0}", vErrors.elementAt(nErrorIndex));
                vErrorsforallCubes.addAll(vErrors);
            }
        }
        return vErrorsforallCubes;
    }
    ////////////////////////////////////////////////////////////
    // END Public static utility methods
    ////////////////////////////////////////////////////////////

    public void invalidateUnusedCache() { //We dont invalidate the dimension cache because there can only be 20 dimensions
        //dimMembersCache.invalidateUnusedCache();
        accountCache.invalidateUnusedCache();
        entityCache.invalidateUnusedCache();

    }

    public synchronized void changeEventOccured(HspChangeEvent hspChangeEvent) {
        if (hspChangeEvent == null)
            return;

        Class changedClass = hspChangeEvent.getSourceAsClass();
        Object source = hspChangeEvent.getSourceObject();

        if (source != null && source instanceof HspPMDimension) {
            // When a PM Dimension is saved, this event listener is called with HspObject, HspDimension, HspMember as changed classes
            // as well since HspPMDimension extends HspDimension. The following check ensures that the cache roots reset and custom
            // handler recreation happens only once after the PM dimension has been updated in the cache by the HspUCChangeEventHandler.
            // HspPMDimension.class has 2 listeners, HspUCChangeEventHandler and this (HspDEBDBImpl) with this being the 2nd one.
            if (changedClass == HspPMDimension.class) {
                resetCacheRoots();
                recreateCustomMemberHandlers();
            }
            return;
        }

        //If a dimension was changed as a member, make sure to flush the dimension caches.
        if (changedClass == HspMember.class || changedClass == HspEntireApplication.class) {
            HspMember member = (HspMember)source;
            if (source != null && member.getDimId() == member.getId()) {
                changedClass = HspDimension.class;
                source = null;
            }
        }

        if (changedClass == HspDimension.class || changedClass == HspAttributeDimension.class || changedClass == HspEntireApplication.class) {
            //Invalidate the Dimension cache.
            dimensionCache.invalidateCache();
            hiddenDimensionsCache.invalidateCache();
            metricDimensionCache.invalidateCache();
            attributeDimensionCache.invalidateCache();
            replacementDimensionCache.invalidateCache();
            //            pmDimensionCache.invalidateCache();
            dpDimensionCache.invalidateCache();
            brScenarioVersionCache.invalidateCache();
            mbrOnFlyDetailCache.invalidateCache();
            mbrPTPropsCache.invalidateCache();
            //Reset the roots of all dimensions
            resetCacheRoots();
            //Invalidate each member cache incase they copy elements from their dimension
            invalidateMemberCaches();
            recreateCustomMemberHandlers();
            regenerateDimIndexCache();
        }
        if (changedClass == HspDimension.class || changedClass == HspEntireApplication.class) {
            synchronized (dimIdsEnabledForEnumsLock) {
                dimIdsEnabledForEnumsByPlanType = null;
            }
        }

        if (changedClass == HspEntireApplication.class) {
            //when application refreshed, clear all dimensions memnor map
            memnorTimedMap.clear();
        }

    }

    protected void invalidateMemberCaches() {
        synchronized (memberCacheHash) {
            Enumeration<GenericCache<? extends HspMember>> caches = memberCacheHash.elements();
            while (caches.hasMoreElements()) {
                GenericCache<? extends HspMember> cache = caches.nextElement();
                if (cache != null)
                    cache.invalidateCache();
            }
        }
    }

    /* get xref for a particular account */

    public String getXREF(int accountId, int planType, int sessionId) {
        String xref = "";
        boolean autoLink = false;
        String mDimName = null, cubeAlias = null;
        try {
            HspSystemCfg cfg = hspJS.getSystemCfg();
            if (cfg != null)
                autoLink = cfg.useAutoLinks();
            HspAccount hspAccount = accountCache.getObject(accountId);
            if (hspAccount.getDataStorage() == HspConstants.kDataStorageLabelOnly)
                return null;
            String mbrName = hspAccount.getMemberName();
            int sourcePType = hspAccount.getSrcPlanType();
            Vector<HspCube> cubes = this.getCubes(sessionId);
            for (int z = 0; z < cubes.size(); z++) {
                HspCube hC = cubes.elementAt(z);
                int cubeType = hC.getPlanType();
                if (cubeType == sourcePType) {
                    cubeAlias = hC.getLocationAlias();
                    break;
                }
            }

            if (sourcePType != planType && ((sourcePType != moduleDimensionAdapter.getWorkforcePlanType()) /*HspConstants.kCubeTypeWorkforce*/ || autoLink)) {
                Vector<HspDimension> dimensions = this.getAllDimensions(planType, sessionId);
                for (int i = 0; i < dimensions.size(); i++) {
                    HspDimension hspDim = dimensions.elementAt(i);
                    //If it is used in the current plantype and not used in the source plantype
                    //Note: Only custom dimensions would satisfy this condition as default dimension belongs
                    //to all plan types
                    if (((hspDim.getUsedIn() & planType) > 0) && ((hspDim.getUsedIn() & sourcePType) <= 0)) {
                        mDimName = hspDim.getObjectName();
                        xref += "\"" + mDimName + "\"->";
                    }
                }
                xref = xref + "\"" + mbrName + "\"= @XREF(\"" + cubeAlias + "\",\"" + mbrName + "\");";
            }
        } catch (Exception e) {
            logger.finer("Exception in getXREF: ", e);
        }
        return xref;
    }

    /* setting mbrFx flag */

    public void setMbrFx(boolean mbrFxFlag, HspMember hspMem, int sessionId) {
        try {
            HspMember clonedMbr = (HspMember)hspMem.cloneForUpdate();
            clonedMbr.setHasMbrFx(mbrFxFlag);
            this.saveMember(clonedMbr, sessionId);
        } catch (Exception e) {
            logger.finer("Exception in setMbrFx: ", e);
        }
    }

    private Vector<HspDimension> getDimVector(int planType, int sessionId) {
        Vector<HspDimension> dimObj = new Vector<HspDimension>();
        try {
            Vector<HspDimension> hdimensions = this.getHiddenDimensions(planType, sessionId);
            for (int i = 0; i < hdimensions.size(); i++) {
                HspDimension hspDim = hdimensions.elementAt(i);
                if (hspDim != null) {
                    if ((hspDim.getUsedIn() & planType) == planType) {
                        dimObj.addElement(hdimensions.elementAt(i));
                    }
                }
            }
            Vector<HspDimension> dimensions = this.getAllDimensions(planType, sessionId);
            for (int j = 0; j < dimensions.size(); j++) {
                HspDimension hspDim = dimensions.elementAt(j);
                if (hspDim != null) {
                    if ((hspDim.getUsedIn() & planType) == planType) {
                        dimObj.addElement(dimensions.elementAt(j));
                    }
                }
            }
        } catch (Exception e) {
            logger.finer("Exception in getting Dimensions", e);
        }
        return dimObj;
    }

    /* get dimension name given position */

    public Vector<HspDimension> getDimNameByPosition(double position, int planType, int sessionId) {
        Vector<HspDimension> dims = this.getDimVector(planType, sessionId);
        HspDimension hDim;
        Vector<HspDimension> dimName = new Vector<HspDimension>();
        for (int i = 0; i < dims.size(); i++) {
            hDim = dims.elementAt(i);
            if (hDim.getPosition(planType) == position) {
                dimName.addElement(hDim);
                //break; Not sure since vector is return type
                // I am assuming two dimension can have same position otherwise we can break here
            }
        }
        return dimName;
    }

    /* update old name in hspObject */

    public void updateObject(int sessionId, HspObject obj) throws Exception {
        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        HspObjectAction objAction = new HspObjectAction();
        obj.setOldName(obj.getObjectName());
        actionSet.addAction(objAction, HspActionSet.UPDATE, obj);
        actionSet.doActions();
    }

    public Vector<HspMember> getMemberNameIdVector() throws SQLException {

        Vector<HspMember> memberNameIdVec = null;
        Connection con = null;
        try {
            con = hspSQL.getConnection();
            memberNameIdVec = hspSQL.executeQuery("SQL_GET_MEMBER_NAME_IDS", con, HspMember.class);
        } finally {
            if (con != null)
                hspSQL.releaseConnection(con);
        }

        return memberNameIdVec;

    }

    public int getNumberNonFlowTimeBalanceAccounts(int planType) {
        int count = 0;
        Connection con = null;
        try {
            con = hspSQL.getConnection();
            String[] params = new String[] { Integer.toString(planType) };

            Vector v = hspSQL.executeQuery("SQL_GET_NUM_TB_ACCOUNTS", params, con, Properties.class);
            Properties p = (Properties)v.elementAt(0);

            count = ((Number)p.get("TBCOUNT")).intValue();

        } catch (Exception e) {
            count = 0;
        } finally {
            if (con != null)
                hspSQL.releaseConnection(con);
        }

        return count;

    }

    public HspDimension getHiddenDimensionObject(String dimName) {

        return hiddenDimensionsCache.getObject(dimName);
    }

    //    public void syncSmartListsForMembers(Set<HspMember> mbrs, int planType, int sessionId) throws Exception {
    //        if (HspUtils.isNullOrEmpty(mbrs))
    //            return;
    //        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
    //        HspActionSet actionSet = new HspActionSet(hspSQL, user);
    //        HspAliasTable aliasTable = hspAlsDB.getAliasTable(HspConstants.MBR_DRIVEN_SL_ALIAS_TABLE);
    //        if (aliasTable == null) {
    //            aliasTable = new HspAliasTable();
    //            aliasTable.setObjectType(HspConstants.gObjType_AliasTable);
    //            aliasTable.setParentId(HspConstants.gFolder_Aliases);
    //            aliasTable.setObjectName(HspConstants.MBR_DRIVEN_SL_ALIAS_TABLE);
    //            hspAlsDB.saveAliasTable(aliasTable, sessionId);
    //        }
    //        int alsTableId = aliasTable.getId();
    //        Set<Integer> slAliasMbrs = new HashSet<Integer>();
    //        Vector<HspAlias> existingAliases = hspAlsDB.getAliases(alsTableId);
    //        for (HspAlias alias : existingAliases) {
    //            slAliasMbrs.add(Integer.valueOf(alias.getObjectName()));
    //        }
    //        for (HspMember mbr : mbrs) {
    //            if(mbr.isSharedMember())continue;
    //            //updateMemberAliases(actionSet, mbr, new Object[][] { new Object[] { alsTableId, String.valueOf(mbr.getId()) } }, true, sessionId);
    //            List<HspMember> lev0Mbrs = mbr.getLevelZeroDescendants(planType);
    //            if (!HspUtils.isNullOrEmpty(lev0Mbrs)) {
    //                HspEnumeration mbrDerivedEnum = getMemberDerivedSL(mbr, planType); // TODO: if we decide to bind a plan type to enum, we should probably only create entries for members used in the correct plan type
    //                if (mbrDerivedEnum != null) {
    //                    mbrDerivedEnum = (HspEnumeration)mbrDerivedEnum.cloneForUpdate();
    //                    List<HspEnumeration.Entry> entries = new ArrayList<HspEnumeration.Entry>(lev0Mbrs.size());
    //                    int i = 0;
    //                    Set<String> uniqueEntries = new HashSet<String>();
    //                    for (HspMember lev0Mbr : lev0Mbrs) {
    //                        //if(lev0Mbr.isSharedMember()) continue;
    //                        if(uniqueEntries.contains(lev0Mbr.getObjectName()))continue; // Due to shared members there could be duplicates
    //                        HspEnumeration.Entry entry = new HspEnumeration.Entry();
    //                        entry.setEnumerationId(mbrDerivedEnum.getIdForCache());
    //                        entry.setEntryId(lev0Mbr.getId());
    //                        entry.setName(makeValidName(lev0Mbr.getObjectName()));
    //                        // Use Description if present as label, else look for alias or use member name
    //                        HspAlias existingAlias = hspAlsDB.getAlias(lev0Mbr);
    //                        entry.setLabel(!HspUtils.isNullOrEmpty(lev0Mbr.getDescription()) ? lev0Mbr.getDescription() : (existingAlias != null ? existingAlias.getName() : lev0Mbr.getObjectName()));
    //                        entries.add(entry);
    //                        if(!slAliasMbrs.contains(lev0Mbr.getId())) {
    //                            slAliasMbrs.add(lev0Mbr.getId());
    //                            List<Object[]> aliases = new ArrayList<Object[]>(1);
    //                            aliases.add( new Object[] { alsTableId, String.valueOf(lev0Mbr.getId())});
    ////                            updateMemberAliases(actionSet, lev0Mbr, aliases.toArray(new Object[aliases.size()][2]), true, sessionId);
    //                            addMemberAliases(actionSet, lev0Mbr, aliases.toArray(new Object[aliases.size()][2]));
    //                        }
    //                        //entry.makeReadOnly();
    //                        uniqueEntries.add(lev0Mbr.getObjectName());
    //                        i++;
    //                    }
    //                    mbrDerivedEnum.setEntries(entries.toArray(new HspEnumeration.Entry[entries.size()]));
    //                    //System.out.println("Enumeration " + mbrDerivedEnum.getName() + ", number entries being added " + mbrDerivedEnum.getEntries().length);
    //                    //mbrDerivedEnum.makeReadOnly(); //TODO figure out how to make it readonly. The action class needs it to be writable
    //                    //actionSet.addAction(new HspEnumerationAction(), HspActionSet.UPDATE, mbrDerivedEnum);
    //                    updateEnumeration(mbrDerivedEnum, actionSet, sessionId);
    //                }
    //            }
    //        }
    //        actionSet.doActions();
    //    }


    private String makeValidName(String name) throws HspRuntimeException {
        StringBuilder validName = new StringBuilder();
        if (!Character.isJavaIdentifierStart(name.charAt(0))) {
            validName.append("_");
            validName.append(name.charAt(0));
        } else {
            validName.append(name.charAt(0));
        }
        int length = Math.min(name.length(), HspConstants.FM_MAX_NAME_LENGTH);
        for (int i = 1; i < length; i++) {
            if (!Character.isJavaIdentifierPart(name.charAt(i))) {
                validName.append("_");
            } else {
                validName.append(name.charAt(i));
            }
        }
        return validName.toString();
    }

    public void updateMembersPostCubeRefresh(int sessionId) throws Exception {
        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        HspSystemCfg systemCfg = hspJS.getSystemCfg();
        HspUtils.verifyArgumentNotNull(systemCfg, "systemCfg");
        systemCfg = (HspSystemCfg)(systemCfg.cloneForUpdate());
        systemCfg.setOtlchgd(false);
        actionSet.addAction(new HspSystemCfgAction(), HspActionSet.UPDATE, systemCfg);
        actionSet.addAction(new HspCubeRefreshCleanupAction(systemCfg), HspActionSet.CUSTOM, (HspObject)null);

        List<Integer> dimIds = new ArrayList<Integer>(Arrays.asList(ArrayUtils.toObject(getOrderedDimIds())));
        Vector<HspDimension> dims = getHiddenDimensions(HspConstants.PLAN_TYPE_ALL, sessionId);
        for (HspDimension dim : dims) {
            dimIds.add(dim.getDimId());
        }
        for (int dimId : dimIds) {
            HspMemberOrderUsage orderUsage = new HspMemberOrderUsage();
            orderUsage.setDimId(dimId);
            orderUsage.setOrderUsed(getDimMembers(dimId, false, sessionId).size());
            actionSet.addAction(new HspMemberOrderUsageAction(), HspActionSet.ADD, orderUsage);
        }


        //Generate the MF for consolidatedData
        HspMember consolidatedData = getMemberByName(HspConstants.MEMBER_NAME_HSP_AGGREGATE);
        if (consolidatedData != null) {
            Vector<HspMemberPTProps> props = mbrPTPropsCache.getObjects(mbrPTPropsMemberIdKeyDef, mbrPTPropsMemberIdKeyDef.createKeyFromId(consolidatedData.getId()));
            //Consolidated data will have only a default formula, propogate the same across all plan types
            if (props != null && props.size() > 0) {
                for (HspCube cube : getCubes(sessionId)) {
                    if (!cube.isASOCube() && hspJS.getSystemCfg().isSandboxEnabled(cube.getPlanType())) {
                        String formula = HspMemberHelper.getFormulaForConsolidatedData(this, sessionId, cube.getPlanType());
                        HspMemberPTProps memberProps = (HspMemberPTProps)props.get(0).cloneForUpdate();
                        memberProps.setFormula(formula);
                        memberProps.setPlanType(cube.getPlanType());
                        actionSet.addAction(new HspMemberPTPropsAction(), HspActionSet.UPDATE_ADDING_IF_NEEDED, memberProps);
                    }
                }
            }

        }

        /*if(hspSystemConfig.isSimpleMultiCurrency()){
            //Update member formula for all Reporting currencies.
            HspMember repCurrs = getMemberByName(HspSimpleCurrencyUtils.MEMBER_NAME_HSP_REPORTING_CURRENCIES);
            Vector<HspMember> children = repCurrs.getChildren();

            HspCurDB hspCurDB = hspJS.getCurDB(sessionId);
            Vector<HspCurrency> defaultCurrencies = hspCurDB.getDefaultCurrency(sessionId);
            String defaultInputCurrencyName = defaultCurrencies.get(0).getMemberName();
            String defaultReportingCurrancyName = defaultInputCurrencyName + HspSimpleCurrencyUtils.STR_REPORTING;

            Vector<HspMember> inputCurrencies = getMemberByName(HspSimpleCurrencyUtils.MEMBER_NAME_HSP_INPUT_CURRENCIES).getChildren();

            for(HspMember repCurrency : children){
                Vector<HspMemberPTProps> props = mbrPTPropsCache.getObjects(mbrPTPropsMemberIdKeyDef, mbrPTPropsMemberIdKeyDef.createKeyFromId(repCurrency.getId()));
                HspMemberPTProps memberProps = null;
                if(props != null){
                    for (HspCube cube:getCubes(sessionId)){
                        if(!cube.isASOCube() && !RegistryManager.isFCCSService()){
                            boolean isDefaultRptCurrency = repCurrency.getName().equals(defaultReportingCurrancyName);
                            Vector customDimensions =  getCustomDimensions(cube.getPlanType(), sessionId);
                            String formula = HspMemberHelper.getFormulaForNewCustomCurrency(inputCurrencies , customDimensions, repCurrency.getName(), defaultInputCurrencyName, isDefaultRptCurrency);
                            if( props.size() > 0) {
                             memberProps = (HspMemberPTProps)props.get(0).cloneForUpdate();
                            } else {
                                memberProps = new HspMemberPTProps();
                                memberProps.setId(repCurrency.getId());
                            }
                            memberProps.setPlanType(cube.getPlanType());
                            memberProps.setDataStorage(HspConstants.kDataStorageDynamic);
                            memberProps.setFormula(formula);
                            actionSet.addAction(new HspMemberPTPropsAction(), HspActionSet.UPDATE_ADDING_IF_NEEDED, memberProps);
                        }
                    }
                }

            }

        }*/
        actionSet.doActions();
    }

    public boolean isDTSMemberEnabled(String dtsMbrName) {
        HspMember mbr = getDimMember(HspConstants.kDimensionTimePeriod, dtsMbrName);
        if (mbr != null && (mbr instanceof HspTimePeriod)) {
            return (((HspTimePeriod)mbr).getType() == HspConstants.DTS_TP_TYPE && mbr.getDTSGeneration() > 0);
        }
        return false;
    }

    public static final HspAttributeDimension createAttributeDimension(String name, int attributeType, int baseDimensionId, int usedIn) {
        HspAttributeDimension dimension = new HspAttributeDimension();
        dimension.setObjectName(name);
        dimension.setBaseDimId(baseDimensionId);
        dimension.setParentId(HspConstants.gFolder_AttrDims);
        dimension.setObjectType(HspConstants.gObjType_AttributeDim);
        dimension.setDimType(HspConstants.kDimTypeAttribute); //HspConstants.kDimTypeAttribute
        dimension.setDataStorage(HspConstants.kDataStorageNeverShare);
        dimension.setConsolOp(HspConstants.PLAN_TYPE_ALL, HspConstants.kDataConsolIgnore);
        dimension.setUsedIn(usedIn);
        dimension.setDensity(HspConstants.kDataDensitySparse);
        dimension.setDensity(HspConstants.PLAN_TYPE_ALL, HspConstants.kDataDensitySparse);
        dimension.setEnforceSecurity(false);
        dimension.setAttributeType(attributeType);
        return dimension;
    }

    /**
     * This method is to create root member from the Dimension and set
     * all relevant properties
     *
     * @param dim
     * @return HspMember
     */
    public static HspMember createRootMemberFromDimension(HspDimension dim) {
        HspMember root = createMember(dim.getDimId(), dim.getObjectType());

        root.setObjectId(dim.getId());
        root.setObjectName(dim.getName());
        root.setDimId(dim.getDimId());
        root.setUuid(dim.getUuid());
        root.setPosition(dim.getPosition());
        root.setDataStorage(dim.getDataStorage());
        for (int plan = 1; plan <= HspConstants.PLAN_TYPE_ALL; plan = plan << 1) {
            root.setConsolOp(plan, dim.getConsolOp(plan));
        }
        root.setMarkedForDelete(dim.isMarkedForDelete());
        root.setRemovable(dim.getRemovable());
        root.setDescription(dim.getDescription());
        root.setGeneration(dim.getGeneration());
        root.setHasChildren(dim.hasChildren());
        root.setHasMbrFx(dim.hasMbrFx());
        root.setObjectType(dim.getObjectType());
        root.setOldName(dim.getOldName());
        root.setTwopassCalc(dim.getTwopassCalc());
        root.setUsedForConsol(dim.isUsedForConsol());
        root.setBaseMemberId(dim.getBaseMemberId());
        root.setPsMemberId(dim.getPsMemberId());
        root.setEnabledForPM(dim.isEnabledForPM());
        //If the parentId is not set, it will point to itself and cause an infinite loop
        //If the parentId is set to 0, this member cannot be updated insql.
        root.setParentId(dim.getParentId());
        root.setUsedIn(dim.getUsedIn());
        root.setCreated(dim.getCreated());
        root.setModified(dim.getModified());
        root.setMoved(dim.getMoved());

        return root;
    }

    public static HspPMDimension createPMDimension(String dimensionName) {
        // You will need to set used in externally
        HspPMDimension dimension = new HspPMDimension();
        dimension.setObjectName(dimensionName);
        dimension.setEnforceSecurity(false);
        dimension.setDataStorage(HspConstants.kDataStorageStoreData);
        dimension.setObjectType(HspConstants.gObjType_PMDimension);
        dimension.setDimType(HspConstants.kDimTypePMDimension);
        dimension.setParentId(HspConstants.gFolder_Dimensions);
        dimension.setTwopassCalc(false);
        dimension.setDensity(HspConstants.PLAN_TYPE_ALL, HspConstants.kDataDensitySparse);
        dimension.setConsolOp(HspConstants.PLAN_TYPE_ALL, HspConstants.kDataConsolIgnore);
        dimension.setUsedIn(1); //(getUsedInForAllCubes(sessionId));          //todo: supply  correct plan types
        dimension.setDimEditor(true);
        return dimension;
        //        delete from hsp_member where member_id = 60448;
        //        delete from hsp_dimension where dim_id = 60448;
        //        delete from hsp_object where object_id = 60448;

    }

    public static HspDPDimension createDPimension(String dimensionName) {
        // You will need to set used in externally
        HspDPDimension dimension = new HspDPDimension();
        dimension.setObjectName(dimensionName);
        dimension.setEnforceSecurity(false);
        dimension.setDataStorage(HspConstants.kDataStorageStoreData);
        dimension.setObjectType(HspConstants.gObjType_DPDimension);
        dimension.setDimType(HspConstants.kDimTypeDPDimension);
        dimension.setParentId(HspConstants.gFolder_Dimensions);
        dimension.setTwopassCalc(false);
        dimension.setDensity(HspConstants.PLAN_TYPE_ALL, HspConstants.kDataDensitySparse);
        dimension.setConsolOp(HspConstants.PLAN_TYPE_ALL, HspConstants.kDataConsolIgnore);
        dimension.setUsedIn(1); //(getUsedInForAllCubes(sessionId));          //todo: supply  correct plan types
        dimension.setDimEditor(true);
        return dimension;
        //        delete from hsp_member where member_id = 60448;
        //        delete from hsp_dimension where dim_id = 60448;
        //        delete from hsp_object where object_id = 60448;

    }

    public Vector<HspLineItemMember> getAllLineItemMembres() {
        return lineItemMemberCache.getUnfilteredCache();
    }

    public Vector<HspLineItemMember> getLineItemMembers(int baseDimId) {
        return lineItemMemberCache.getObjects(lineItemMemberBaseDimIdKeyDef, lineItemMemberBaseDimIdKeyDef.createKeyFromBaseDimId(baseDimId));
    }

    public HspDimension getDimFromNameOrAlias(String dim, int planType, int alsTblId, int sessionId) {

        HspDimension hspDim = getDimRoot(planType, dim);

        if (hspDim == null)
            hspDim = getAttributeDimension(dim);

        //method below can throw duplicate alias exception
        if (hspDim == null && alsTblId != -1)
            hspDim = getDimensionFromAlias(planType, dim, alsTblId, sessionId);

        return hspDim;
    }

    public HspMember getMbrFromNameOrAlias(int dimId, String mbr, int alsTblId) {

        HspMember hspMbr = null;

        if (alsTblId != -1) {
            hspMbr = getMemberIdAllowMbrNameMatch(dimId, mbr, alsTblId);
        }

        if (hspMbr == null)
            hspMbr = getDimMember(dimId, mbr);

        return hspMbr;
    }

    public HspExternalServer getEssbaseServer(int reportingServerId, int sessionId) {
        hspStateMgr.verify(sessionId);
        return essbaseServerCache.getObject(reportingServerId);
    }

    public HspExternalServer getEssbaseServer(String reportingServerName, int sessionId) {
        hspStateMgr.verify(sessionId);
        return essbaseServerCache.getObject(reportingServerName);
    }

    public HspExternalServer getEssbaseServerByDisplayName(String displayName, int sessionId) {
        hspStateMgr.verify(sessionId);
        return essbaseServerCache.getObject(displayNameKeyDef, displayName);
    }

    public Vector<HspExternalServer> getEssbaseServers(int sessionId) {
        hspStateMgr.verify(sessionId);
        return essbaseServerCache.getUnfilteredCache();
    }

    public synchronized void addExternalServer(HspExternalServer reportingCubeServer, int sessionId) throws Exception {
        HspUtils.verifyArgumentNotNull(reportingCubeServer, "reportingCubeServer");
        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        if (reportingCubeServer.isDefaultEssbaseServer()) {
            throw new HspRuntimeException("MSG_CL_ERROR__CANNOT_ADD_DEFAULT_ESSBASE_SERVER");


        }
        HspExternalServer dupEss = getEssbaseServer(reportingCubeServer.getServer(), sessionId);
        if (dupEss != null)
            throw new DuplicateObjectException("MSG_SQL_DUPLICATE_ESSBASE_SERVER", reportingCubeServer.getServer());
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        actionSet.addAction(new HspExternalServerAction(), HspActionSet.ADD, reportingCubeServer);
        actionSet.doActions();
    }

    public synchronized void updateExternalServer(HspExternalServer reportingCubeServer, int sessionId) throws Exception {
        HspUtils.verifyArgumentNotNull(reportingCubeServer, "reportingCubeServer");
        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        if (reportingCubeServer.isDefaultEssbaseServer()) {
            throw new HspRuntimeException("MSG_CL_ERROR__CANNOT_EDIT_DEFAULT_ESSBASE_SERVER");
        }
        HspExternalServer dupEss = getEssbaseServer(reportingCubeServer.getServer(), sessionId);
        if (dupEss != null && dupEss.getServerId() != reportingCubeServer.getServerId())
            throw new DuplicateObjectException("MSG_SQL_DUPLICATE_ESSBASE_SERVER", reportingCubeServer.getServer());
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        actionSet.addAction(new HspExternalServerAction(), HspActionSet.UPDATE, reportingCubeServer);
        actionSet.doActions();
    }

    public synchronized void deleteExternalServer(HspExternalServer reportingCubeServer, int sessionId) throws Exception {
        HspUtils.verifyArgumentNotNull(reportingCubeServer, "reportingCubeServer");
        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));

        if (reportingCubeServer.isDefaultEssbaseServer()) {
            throw new HspRuntimeException("MSG_CL_ERROR__CANNOT_DELETE_DEFAULT_ESSBASE_SERVER");
        }
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        actionSet.addAction(new HspExternalServerAction(), HspActionSet.DELETE, reportingCubeServer);
        actionSet.doActions();
    }

    private ModuleDimensionAdapter moduleDimensionAdapter;

    private final Hashtable<Integer, GenericCache<? extends HspMember>> memberCacheHash = new Hashtable<Integer, GenericCache<? extends HspMember>>();
    private HspUpdateableCache<HspDimension> dimensionCache;
    private HspUpdateableCache<HspAttributeDimension> attributeDimensionCache;
    //private MultiPartCache dimMembersCache;
    private HspUpdateableCache<HspCube> cubeCache;
    private HspUpdateableCache<HspCubeSLMapping> cubeSLMappingCache;
    private HspUpdateableCache<HspDPCubeMapping> cubeMappingCache;
    private HspUpdateableCache<HspExternalServer> essbaseServerCache;
    //private HspUpdateableCache<HspEssbaseCube> essbaseCubeCache;
    private HspUpdateableCache<HspScenario> scenarioCache;
    private HspUpdateableCache<HspVersion> versionCache;
    private HspUpdateableCache<HspAccount> accountCache;
    private HspUpdateableCache<HspEntity> entityCache;
    private HspUpdateableCache<HspTimePeriod> timePeriodCache;
    private HspUpdateableCache<HspYear> yearCache;
    private HspUpdateableCache<HspCurrency> fxTablesInfoCache;
    private HspUpdateableCache<HspCurrency> currencyCache;
    //private HspUpdateableCache labelDynamicMembersCache;
    private HspUpdateableCache<HspMetric> metricCache;
    private HspUpdateableCache<HspDimension> hiddenDimensionsCache;
    private HspUpdateableCache<HspDimension> metricDimensionCache;
    private HspUpdateableCache<HspReplacementDimension> replacementDimensionCache;
    private HspUpdateableCache<HspPMDimension> pmDimensionCache;
    private HspUpdateableCache<HspDPDimension> dpDimensionCache;
    private HspUpdateableCache<HspDPSVBRBinding> brScenarioVersionCache;
    private MultiPartCache<HspObjectNote> objectNotesCache;
    private HspUpdateableCache<HspMemberOnFlyDetail> mbrOnFlyDetailCache;
    private GenericCache<HspMemberOnFlyBucket> mbrOnFlyBucketCache;
    private HspUpdateableCache<HspMemberPTProps> mbrPTPropsCache;

    private HspUpdateableCache<HspUserVariable> userVariableCache;
    private HspUpdateableCache<HspUserVariableValue> userVariableValueCache;
    private HspUpdateableCache<HspEnumeration> enumCache;
    private HspGeneratedEnumCacheLoader generatedEnumCacheLoader;
    private GenericCache<HspEnumeration> generatedEnumCache;
    private EnumMemberIdKeyDef enumMbrIdKeyDef = EnumMemberIdKeyDef.ENUM_MEMBER_ID_KEY;
    private final Map<Integer, Long> dimMembersImpactedTimeMap = Collections.synchronizedMap(new HashMap<Integer, Long>());
    private final Map<Integer, Long> memberDrivenSmartListRebuiltTimeMap = Collections.synchronizedMap(new HashMap<Integer, Long>());
    //Note: 1. dimId, 2. mbrId, 3. memnor
    private Map<Integer, Map<Integer, Integer>> memnorTimedMap = Collections.synchronizedMap(new TTLMap(60000));

    private HspUpdateableCache<HspUDA> udaCache;
    private HspUpdateableCache<HspUDABinding> udaBindingCache;
    private GenericCache<HspDriverMember> driverMemberCache;
    private HspUpdateableCache<HspLineItemMember> lineItemMemberCache;

    //Excel meta data support
    private HspUpdateableCache<HspDimension> virtualDimensionsCache;
    private GenericCache<HspEnumeration> generatedCurrencyEnumsCache;
    private GenericCache<HspEnumeration> generatedSmartListsEnumsCache;
    private GenericCache<HspEnumeration> generatedStaticMbrPropertiesEnumsCache;
    private GenericCache<HspEnumeration> generatedFXTablesEnumsCache;
    private GenericCache<HspEnumeration> generatedYearsEnumsCache;
    private GenericCache<HspEnumeration> generatedSourcePlanTypeEnumsCache;
    private GenericCache<HspEnumeration> generatedPeriodsEnumsCache;
    private GenericCache<HspEnumeration> generatedAtttribDimMbrsEnumCache;

    private JDBCCacheLoader<HspLock> objectLockCacheLoader;


    private CachedObjectIdKeyDef orderedObjectIdKeyDef = CachedObjectIdKeyDef.ORDERED_OBJECT_ID_KEY_DEF;
    private CachedObjectParentIdKeyDef cachedObjectParentIdKeyDef = CachedObjectParentIdKeyDef.CACHED_OBJECT_PARENT_ID_KEY;
    private HspDPCubeMappingKeyDef dpCubeMappingFromToKeyDef = HspDPCubeMappingKeyDef.DP_CUBE_MAPPING_FROM_TO_KEY_DEF;
    private CachedObjectDefaultKeyDef cachedObjectDefaultKeyDef = CachedObjectDefaultKeyDef.CACHED_OBJECT_DEFAULT_KEY;
    private MemberPsIdKeyDef memberPsIdKeyDef = MemberPsIdKeyDef.MEMBER_PS_ID_KEY;
    private AttributeReferenceMemberIdKeyDef attrMemberRefKeyDef = AttributeReferenceMemberIdKeyDef.ATTRIBUTE_REFERENCE_MEMBER_ID_KEY_DEF;
    private MemberBaseMemberIdKeyDef memberBaseMemberIdKeyDef = MemberBaseMemberIdKeyDef.MEMBER_BASE_MEMBER_ID_KEY_DEF;
    //    private MemberIsDynamicChildEnabledKeyDef mbrIsDynamicChildEnabledKeyDef = MemberIsDynamicChildEnabledKeyDef.MEMBER_IS_DYNAMIC_CHILD_ENABLED_KEY;
    private HspObjectOldNameKeyDef hspObjectOldNameKeyDef = HspObjectOldNameKeyDef.HSP_OBJECT_OLD_NAME_KEY;

    private AccountSubAccountKeyDef accountSubAccountKeyDef = AccountSubAccountKeyDef.ACCOUNT_SUB_ACCOUNT_KEY_DEF;
    private EntityRequisitionNumberKeyDef entityRequisitionNumberKeyDef = EntityRequisitionNumberKeyDef.ENTITY_REQUISITION_NUMBER_KEY;

    private HspBRScenarioVersionKeyDef brScenarioIdVersionIdKeyDef = HspBRScenarioVersionKeyDef.DP_BR_VERSION_BY_DP_MEMBER_ID_SCENARIO_ID_KEY_DEF;
    private HspDPMemberKeyDef dpMemberKeyDef = HspDPMemberKeyDef.DP_MEMBER__KEY_DEF;
    private DPDimByDPTypeIdKeyDef dpDimKeyDef = DPDimByDPTypeIdKeyDef.DP_DIM_BY_DPTYPE_ID_KEY_DEF;
    private DPDimByScenarioKeyDef dpDimScenarioKeyDef = DPDimByScenarioKeyDef.DP_DIM_BY_SCENARIO_KEY_DEF;
    private CachedObjectIdKeyDef dpNoteAttachmentIdKeyDef = CachedObjectIdKeyDef.CACHED_OBJECT_ID_KEY;
    private HspObjectNoteKeyDef dpNoteAttachmentKeyDef = HspObjectNoteKeyDef.DP_NOTE_BY_DP_MEMBER_ID_KEY_DEF;
    private HspObjectNoteByTypeKeyDef objNoteTypeKeyDef = HspObjectNoteByTypeKeyDef.OBJECT_NOTE_BY_TYPE_KEY_DEF;
    private static final HspObjectNoteMPCKeyGenerator objectNoteMPCKeyGenerator = new HspObjectNoteMPCKeyGenerator();

    //private HspDPSharingEntitiesKeyDef dpSharingEntitiesKeyDef = HspDPSharingEntitiesKeyDef.DP_ENTITY_DP_MEMBER_ID_KEY_DEF;
    //private DimTypeKeyDef dimTypeKeyDef = DimTypeKeyDef.DIM_TYPE_KEY_DEF;
    private UserVariableDimIdNameKeyDef userVariableDimIdNameKeyDef = UserVariableDimIdNameKeyDef.USER_VARIABLE_DIM_ID_NAME_KEY_DEF;
    private UserVariableDimIdKeyDef userVariableDimIdKeyDef = UserVariableDimIdKeyDef.USER_VARIABLE_DIM_ID_KEY_DEF;
    private UserVariableValueUserIdVariableIdKeyDef userVariableValueUserIdVariableIdKeyDef = UserVariableValueUserIdVariableIdKeyDef.USER_VARIABLE_VALUE_USER_ID_VARIABLE_ID_KEY_DEF;
    private UserVariableValueVariableIdKeyDef userVariableValueVariableIdKeyDef = UserVariableValueVariableIdKeyDef.USER_VARIABLE_VALUE_VARIABLE_ID_KEY_DEF;
    private CachedObjectKeyDef enumKeyDef = CachedObjectIdKeyDef.CACHED_OBJECT_ID_KEY;
    private EnumerationEntryEnumIdKeyDef enumEntryKeyDef = EnumerationEntryEnumIdKeyDef.ENUMERATION_ENTRY_ENUM_ID_KEY_DEF;
    private CachedObjectKeyDef displayNameKeyDef = HspDisplayNameKeyDef.EXT_SERVER_DISPLAY_NAME_KEY_DEF;
    private UDAKeyDef udaKeyDef = UDAKeyDef.UDA_KEY_DEF;
    private UDADimIdKeyDef udaDimIdKeyDef = UDADimIdKeyDef.UDA_DIM_ID_KEY_DEF;
    private UDADimIdUDAValueKeyDef udaDimIdUDAValueKeyDef = UDADimIdUDAValueKeyDef.UDA_DIM_ID_UDA_VALUE_KEY_DEF;
    private UDABindingMemberIdUDAIdKeyDef udaBindingKeyDef = UDABindingMemberIdUDAIdKeyDef.UDA_BINDING_MEMBER_ID_UDA_ID_KEY_DEF;
    private UDABindingMemberIdKeyDef udaBindingsForMemberKeyDef = UDABindingMemberIdKeyDef.UDA_BINDING_MEMBER_ID_KEY_DEF;
    private UDABindingUDAIdKeyDef udaBindingsForUDAKeyDef = UDABindingUDAIdKeyDef.UDA_BINDING_UDA_ID_KEY_DEF;
    private DriverMemberBaseDimIdKeyDef driverMemberBaseDimIdKeyDef = DriverMemberBaseDimIdKeyDef.DRIVER_MEMBER_BASE_DIM_ID_KEY_DEF;
    private LineItemMemberBaseDimIdKeyDef lineItemMemberBaseDimIdKeyDef = LineItemMemberBaseDimIdKeyDef.LINE_ITEM_MEMBER_BASE_DIM_ID_KEY_DEF;
    private LineItemMemberMbrSelIdKeyDef lineItemMemberMbrSelIdKeyDef = LineItemMemberMbrSelIdKeyDef.LINE_ITEM_MEMBER_MBR_SEL_ID_KEY_DEF;
    private UserVariableMbrSelIdKeydef userDefMbrSelKeyDef = UserVariableMbrSelIdKeydef.USER_VAR_MEMBER_MBR_SEL_ID_KEY_DEF;
    private EnumMbrSelIdKeyDef enumMbrSelKeyDef = EnumMbrSelIdKeyDef.ENUM_MEMBER_MBR_SEL_ID_KEY_DEF;
    private CachedObjectIdKeyDef dimSelIdKeyDef = CachedObjectIdKeyDef.CACHED_OBJECT_ID_KEY;
    private FormMemberBySelIdKeyDef formMemberParentIdKeyDef = FormMemberBySelIdKeyDef.FORM_MEMBER_SELECTION_BY_SEL_ID_KEY;

    //private MemberOnFlyParentIdKeyDef mbrOnFlyParentIdKeyDef = MemberOnFlyParentIdKeyDef.MEMBER_ON_FLY_PARENT_ID_KEY_DEF;
    private MemberPTPropsMemberIdKeyDef mbrPTPropsMemberIdKeyDef = MemberPTPropsMemberIdKeyDef.MEMBER_PT_PROPS_MEMBER_ID_KEY_DEF;
    private CachedObjectIdKeyDef mbrOnFlyParentIdKeyDef = CachedObjectIdKeyDef.CACHED_OBJECT_ID_KEY;
    private MemberOnFlyBucketParentIdBucketIndexKeyDef mofBucketParentIdBucketIndexKeyDef = MemberOnFlyBucketParentIdBucketIndexKeyDef.MOF_BUCKET_PARENT_ID_BUCKET_INDEX_KEYDEF;
    private MemberOnFlyParentIdCurrentBucketKeyDef mbrOnFlyParentIdCurrentBucketKeyDef = MemberOnFlyParentIdCurrentBucketKeyDef.MEMBER_ON_FLY_PARENT_ID_CURRENT_BUCKET_KEY_DEF;
    private CubeTypeKeyDef cubeTypeKeyDef = CubeTypeKeyDef.CUBE_TYPE_KEY_DEF;
    private MemberPTPropsMemberIdPlanTypeKeyDef mbrPTPropsMemberIdPlanTypeKeyDef = MemberPTPropsMemberIdPlanTypeKeyDef.MEMBER_PT_PROPS_MEMBER_ID_PLAN_TYPE_KEY_DEF;

    private HspDimensionCustomizationMgr hspDimensionCustomizationMgr;

    private HspSecDB hspSecDB;
    private HspJS hspJS;
    private HspSQL hspSQL;
    private HspStateMgr hspStateMgr;
    private HspCurDB hspCurDB;
    private HspPMDB hspPMDB;
    private HspAlsDB hspAlsDB;
    private HspCalDB hspCalDB;
    private HspFMDB hspFMDB;
    private HspPrefDB hspPrefDB;
    private HspSystemCfg hspSystemConfig;
    private HspOLAP hspOLAP;
    private HspHALDB hspHALDB;
    private HspCubeLinkDB hspCubeLinkDB;
    private int internalSessionId;
    private int[] orderedDimIds = null;
    private Map<Integer, int[]> dimIdsEnabledForEnumsByPlanType;
    private Object dimIdsEnabledForEnumsLock = new int[0];
    private HspReportingDimMemberCacheManager hspRepDimMbrCacheMgr;


    private static final String AS_PARAM_CHILDREN_MAP = "HspDEDBImpl.AS_PARAM_CHILDREN_MAP";
    // Whitespace characters we wont tolerate at beginning or end of string
    private static final String[] whiteSpace = { " ", "\t" };

    // characters that are invalid in the first position only
    private static final String[] bad1stChars = { "\'", "(", ")", "+", ",", "-", ".", "<", "=", "@", "_", "{", "|", "}" };

    // characters that are invalid in any position
    // added commas as invalid because of currency conversion REM 10/23/01
    // undid above change - removed commas as invalid
    // backslash is temporarily being disallowed anywhere in a dimension/member name
    // because it is not handled properly in the web ui. When the web ui handles it properly
    // reinstate it as a valid character anywhere but the 1st character.
    private static final String[] badChars = { "\"", "\t", "\\" };

    // reserved words
    private static final String[] reservedWords = { "$$$UNIVERSE$$$", "#MISSING", "#MI" };

    // Calc Script Commands
    private static final String[] calcScriptCommands =
    { "&", "AGG", "ARRAY", "CALC All", "CALC AVERAGE", "CALC DIM", "CALC First", "CALC Last", "CALC TWOPASS", "CCONV", "CLEARBLOCK", "CLEARDATA", "DATACOPY", "Else", "ELSEIF", "EndIf", "ENDFIX", "ENDLOOP", "IF", "FIX", "Loop", "SET", "SET AGGMISSG", "SET CACHE",
      "SET CALCHASHTBL", "SET CLEARUPDATESTATUS", "SET FRMLBOTTOMUP", "SET LOCKBLOCK", "SET MSG", "SET NOTICE", "SET UPDATECALC", "SET UPTOLOCAL", "Var" };
    private static final Hashtable<String, String> calcScriptCommandsHash = new Hashtable<String, String>(calcScriptCommands.length);
    static {
        for (int i = 0; i < calcScriptCommands.length; i++) {
            String key = calcScriptCommands[i].toLowerCase();
            calcScriptCommandsHash.put(key, key);
        }
    }

    // report script commands
    private static final String[] reportScriptCommands =
    { "&", "!", "AFTER", "ALLINSAMEDIM", "ALLSIBLINGS", "ANCESTORS", "ASYM", "ATTRIBUTE", "BEFORE", "BLOCKHEADERS", "BOTTOM", "BRACKETS", "CALCULATE Column", "CALCULATE ROW", "CHILDREN", "CLEARALLROWCALC", "CLEARROWCALC", "COLHEADING", "Column", "COMMAS", "CURHEADING",
      "DECIMAL", "DESCENDANTS", "DIMBOTTOM", "DIMEND", "DIMTOP", "DUPLICATE", "ENDHEADING", "EUROPEAN", "FEEDON", "FIXCOLUMNS", "FORMATCOLUMNS", "HEADING", "IANCESTORS", "ICHILDREN", "IDESCENDANTS", "IMMHEADING", "INCEMPTYROWS", "INCFORMATS", "INCMASK", "INCMISSINGROWS",
      "INCZEROROWS", "INDENT", "INDENTGEN", "IPARENT", "LATEST", "LINK", "LMARGIN", "MASK", "MATCH", "MISSINGTEXT", "NAMESCOL", "NAMESON", "NAMEWIDTH", "NewPage", "NOINDENTGEN", "NOPAGEONDIMENSION", "NOROWREPEAT", "NOSKIPONDIMENSION", "NOUNAMEONDIM", "OFFCOLCALCS",
      "OFFROWCALCS", "OFSAMEGEN", "ONCOLCALCS", "ONROWCALCS", "ONSAMELEVELAS", "Order", "ORDERBY", "OUTALT", "OUTALTMBR", "OUTALTNAMES", "OUTALTSELECT", "OUTMBRALT", "OUTMBRNAMES", "OUTPUT", "Page", "PAGEHEADING", "PAGELENGTH", "PAGEONDIMENSION", "Parent", "PRINTROW",
      "PYRAMIDHEADERS", "QUOTEMBRNAMES", "REMOVECOLCALCS", "RENAME", "RESTRICT", "ROW", "ROWREPEAT", "SAVEANDOUTPUT", "SAVEROW", "Scale", "SETCENTER", "SETROWOP", "SINGLECOLUMN", "Skip", "SKIPONDIMENSION", "SORTALTNAMES", "SORTASC", "SORTDESC", "SORTGEN", "SORTLEVEL",
      "SORTMBRNAMES", "SORTNONE", "SPARSE", "STARTHEADING", "SUPALL", "SUPBRACKETS", "SUPCOLHEADING", "SUPCOMMAS", "SUPCURHEADING", "SUPEMPTYROWS", "SUPEUROPEAN", "SUPFEED", "SUPFORMATS", "SUPHEADING", "SUPMASK", "SUPMISSINGROWS", "SUPNAMES", "SUPOUTPUT", "SUPPAGEHEADING",
      "SUPSHARE", "SUPSHAREOFF", "SUPZEROROWS", "SYM", "TABDELIMIT", "Text", "TODATE", "Top", "UCHARACTERS", "UCOLUMNS", "UDA", "UDATA", "UNAME", "UNAMEONDIMENSION", "UNDERLINECHAR", "UNDERSCORECHAR", "WIDTH", "WITHATTR", "ZEROTEXT" };
    private static final Hashtable<String, String> reportScriptCommandsHash = new Hashtable<String, String>(reportScriptCommands.length);
    //Poputlate the hash
    static {
        for (int i = 0; i < reportScriptCommands.length; i++) {
            String key = reportScriptCommands[i].toLowerCase();
            reportScriptCommandsHash.put(key, key);
        }
    }
    private static final String[] DTSReservedGenNames = { "HISTORY", "YEAR", "SEASON", "PERIOD", "QUARTER", "MONTH", "WEEK", "DAY" };
    private static final String[] DTSReservedMbrNames = { "H-T-D", "Y-T-D", "S-T-D", "P-T-D", "Q-T-D", "M-T-D", "W-T-D", "D-T-D" };

    private static final Hashtable<String, String> DTSGenToMbrNameHash = new Hashtable<String, String>(DTSReservedGenNames.length);
    private static final Hashtable<String, String> DTSMbrNameToGenHash = new Hashtable<String, String>(DTSReservedGenNames.length);
    //Populate the hash
    static {
        for (int i = 0; i < DTSReservedGenNames.length; i++) {
            String dtsGenName = DTSReservedGenNames[i].toLowerCase();

            DTSGenToMbrNameHash.put(dtsGenName, DTSReservedMbrNames[i]);
            DTSMbrNameToGenHash.put(DTSReservedMbrNames[i], dtsGenName);
        }
    }

    private static final short DMNameMaximumLength = 80; // maximum length of dimension member names
    // limit to object_name column of object
    // table: kMaxObjectNameLength
    private static final short DMNameMinimumLength = 1; // Mimimum length of dimension member names
    private static final short AttributeNameMaximumLength = 32; // Maximum length of Attribute Member name
    // Constants used for positioning
    //private static final int kMemberPositionInitial	= 50000;
    private static final int kMemberPositionInitial = 1000000;
    private static final int kMemberPositionIncrement = 10;
    //TODO: Need to check whether all combonations of db's and os's can support this value adequetely
    private static final int kMemberPositionMaximum = java.lang.Integer.MAX_VALUE;
    private static final int ASCENDING = 1;
    private static final int DESCENDING = -1;

    // object description property maximum length
    private static final int ObjectDescriptionMaximumLength = 255;

    //private HspObjectNameComparator comparator;
    private HspChangeEventNotifier notifier;
    private Vector<HspUCCustomMemberChangeEventHandler> customDimHandlers;
    private Vector<HspUCCustomMemberChangeEventHandler> replacementDimHandlers;
    private Vector<HspGCChangeEventHandler> pmDimHandlers;
    private Vector<HspUCChangeEventHandler> dpDimHandlers;

    public String getBRLabel(int brDimMemberId, int dpScenarioId, int brVersionId) {
        HspDPSVBRBinding brInfo = getBRScenarioVersionInfo(brDimMemberId, dpScenarioId, brVersionId);
        if (brInfo != null) {
            HspMember brMbr = hspPMDB.getDPMember(dpScenarioId, brVersionId, brInfo.getDpMemberId()); //getMemberById(brInfo.getDpMemberId());
            return brMbr != null ? brMbr.getObjectName() : null;
        }
        return null;
    }

    public HspDPSVBRBinding getBRScenarioVersionInfo(int brDimMemberId, int dpScenarioId, int brVersionId) {
        Vector brs = brScenarioVersionCache.getObjects(brScenarioIdVersionIdKeyDef, brScenarioIdVersionIdKeyDef.createKeyFromBRScenarioVersion(brDimMemberId, dpScenarioId, brVersionId));
        if (brs.size() == 1)
            return ((HspDPSVBRBinding)brs.get(0));

        return null;
    }

    public int getBRDimId(int sessionId) {
        Vector dimList = getAllDimensions(HspConstants.PLAN_TYPE_ALL, sessionId);
        for (int i = 0; i < dimList.size(); i++) {
            HspDimension dim = (HspDimension)dimList.elementAt(i);
            if (dim.getObjectType() == HspConstants.gObjType_BudgetRequest)
                return dim.getId();
        }
        return -1;
    }

    public void associateBudgetRequestWithEssbaseMember(final HspDPMember member, int sessionId) throws Exception {
        final HspDPDimension dpDim = getDPDimension(member.getDimId());
        final HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        final Class[] fatalExceptions = { HspBRDimMembersExhaustedException.class };
        int brDimMemberId = 0;
        try {
            brDimMemberId = new RepeatableCallable<Integer>(new Callable<Integer>() {
                        public Integer call() throws Exception {
                            HspActionSet actionSet = new HspActionSet(hspSQL, user);
                            HspDPSVBRBinding binding = new HspDPSVBRBinding(member.getIdForCache(), -1, dpDim.getScenarioId(), member.getVersionId());
                            HspDPMember clonedMember = (HspDPMember)member.cloneForUpdate();
                            actionSet.addAction(new HspDPSVBRBindingAction(), HspActionSet.ADD, binding);
                            actionSet.addAction(new HspCopyIdAction("brDimMemberId", "brDimMemberId"), HspActionSet.CUSTOM, new DualVal<Object, Object>(clonedMember, binding));
                            actionSet.addAction(new HspDPMemberAction(), HspActionSet.UPDATE, clonedMember);
                            actionSet.doActions();
                            return binding.getBrDimMemberId();
                        }
                    }, 10, 500, fatalExceptions).call();
        } catch (HspBRDimMembersExhaustedException e) {
            throw e;
        } catch (Exception e) {
            throw new HspRuntimeException("MSG_ASSOCIATION_TO_ESS_MBRID_FAILED", e);
        }
    }


    /**
     * The api updates scenario, version, and request member binding for a DP member if there do not exist a binding
     * for the passed member.
     * The api does nothing if
     * 1. an association already exists for the passed member
     * 2. the passed in DP member has invalid brDimMemberId (less than 1)
     * @param member
     * @param sessionId
     * @throws Exception
     */
    public void updateSVBRAssociation(final HspDPMember member, int sessionId) throws Exception {
        HspUtils.verifyArgumentNotNull(member, "member");
        if (member.getBrDimMemberId() < 1)
            return;

        HspDPDimension dpDim = getDPDimension(member.getDimId());
        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        HspDPSVBRBinding svbrBind = getBRScenarioVersionInfo(member.getBrDimMemberId(), dpDim.getScenarioId(), member.getVersionId());
        if (svbrBind == null) {
            HspActionSet actionSet = new HspActionSet(hspSQL, user);
            HspDPSVBRBinding binding = new HspDPSVBRBinding(member.getIdForCache(), member.getBrDimMemberId(), dpDim.getScenarioId(), member.getVersionId());
            actionSet.addAction(new HspDPSVBRBindingAction(), HspActionSet.ADD, binding);
            actionSet.doActions();
        }
    }

    //    public HspMemberOnFlyDetail getMemberOnTheFlyDetail(int dimId, String parentName) {
    //        Vector<HspMemberOnFlyDetail> details = mbrOnFlyDetailCache.getObjects(mbrOnFlyParentIdKeyDef, mbrOnFlyParentIdKeyDef.createKeyFromMemberOnFlyDetail(dimId));
    //        for (HspMemberOnFlyDetail detail : details) { // TODO this should be a keydef lookup
    //            if (detail.getParentName().equalsIgnoreCase(parentName))
    //                return detail;
    //        }
    //
    //        return null;
    //    }

    public Vector<HspMemberPTProps> getMemberPTProps(int memberId, int sessionId) {
        hspStateMgr.verify(sessionId);
        Vector<HspMemberPTProps> props = mbrPTPropsCache.getObjects(mbrPTPropsMemberIdKeyDef, mbrPTPropsMemberIdKeyDef.createKeyFromId(memberId));
        return props;
    }

    public HspMemberOnFlyDetail getMemberOnTheFlyDetail(int memberId, int sessionId) {
        hspStateMgr.verify(sessionId);
        Vector<HspMemberOnFlyDetail> details = mbrOnFlyDetailCache.getObjects(mbrOnFlyParentIdKeyDef, mbrOnFlyParentIdKeyDef.createKeyFromId(memberId));
        return details != null && details.size() > 0 ? details.get(0) : null;
    }

    private void updateMemberOnTheFlyDetail(HspActionSet actionSet, HspMember member, HspMemberOnFlyDetail mbrOnFlyDetail, HspMemberInfo memberInfo, int sessionId) throws Exception {
        hspStateMgr.verify(sessionId);
        int memberId = member.getId();
        int dimId = member.getDimId();
        HspMemberOnFlyDetail existingMbrOnFlyDetail = getMemberOnTheFlyDetail(memberId, sessionId);
        if (memberInfo != null && existingMbrOnFlyDetail != null)
            memberInfo.setOldMofDetails((HspMemberOnFlyDetail)existingMbrOnFlyDetail.cloneForUpdate());

        if ((mbrOnFlyDetail == null && existingMbrOnFlyDetail == null) || (mbrOnFlyDetail != null && existingMbrOnFlyDetail != null && mbrOnFlyDetail.propertiesEquivalentTo(existingMbrOnFlyDetail)))
            return;

        HspMemberOnFlyDetailAction action = new HspMemberOnFlyDetailAction();
        // If the member has not been added yet, then add an action
        // to copy the id from the member to the mbrOnFlyDetail
        if (member.getId() <= 0 && mbrOnFlyDetail != null)
            actionSet.addAction(new HspCopyIdAction("parentMbrId", "objectId"), HspActionSet.CUSTOM, new DualVal(mbrOnFlyDetail, member));
        if (existingMbrOnFlyDetail == null && mbrOnFlyDetail != null) {
            mbrOnFlyDetail.setParentMbrId(memberId);
            mbrOnFlyDetail.setOldBucketSize(mbrOnFlyDetail.getBucketSize());
            // For unsecured dimension, ignore the incoming creator access mode and always set it to Inherit
            if (!isDimensionSecured(dimId, sessionId))
                mbrOnFlyDetail.setCreatorAccessMode(HspConstants.ACCESS_UNSPECIFIED);
            actionSet.addAction(action, HspActionSet.ADD, mbrOnFlyDetail);
        } else if (existingMbrOnFlyDetail != null && mbrOnFlyDetail != null) {
            // For unsecured dimension, ignore the incoming creator access mode and always set it to Inherit
            if (!isDimensionSecured(dimId, sessionId)) {
                //mbrOnFlyDetail = (HspMemberOnFlyDetail)mbrOnFlyDetail.cloneForUpdate();
                mbrOnFlyDetail.setCreatorAccessMode(HspConstants.ACCESS_UNSPECIFIED);
            }
            actionSet.addAction(action, HspActionSet.UPDATE, mbrOnFlyDetail);
        } else if (existingMbrOnFlyDetail != null && mbrOnFlyDetail == null) {
            actionSet.addAction(action, HspActionSet.DELETE, existingMbrOnFlyDetail.cloneForUpdate());
        }

        if (memberInfo != null && mbrOnFlyDetail != null) {
            //memberInfo.setOldMofDetails((HspMemberOnFlyDetail)existingMbrOnFlyDetail.cloneForUpdate());
            memberInfo.setMofDetails((HspMemberOnFlyDetail)mbrOnFlyDetail.cloneForUpdate());
        }
        //actionSet.doActions();
    }

    private void initializeEssbaseNameForMemberOnTheFly(HspActionSet actionSet, HspMember member, int sessionId) throws Exception {
        HspUtils.verifyArgumentNotNull(actionSet, "actionSet");
        HspMemberOnFlyDetail detail = getMemberOnTheFlyDetail(member.getParentId(), sessionId);
        if (detail == null) // Parent not enabled for dynamic child add
            return;
        HspMember parent = getDimMember(member.getDimId(), member.getParentId());

        // Add a new Usage row which will fail with an integrity violation if any other server grabbed the same bucket first.
        // In this case the entire saveMember() call will be retried
        actionSet.addAction(new HspMemberOnFlyCompositeAction(getBucketUuidDataSource()), HspActionSet.ADD, HspMemberOnFlyCompositeAction.createObject(member, parent, detail));
    }

    private HspMemberOnFlyCompositeAction.BucketUuidDataSource getBucketUuidDataSource() {
        return new HspMemberOnFlyCompositeAction.BucketUuidDataSource() {
            @Override
            public HspMemberOnFlyBucket getMemberOnFlyBucket(HspMember parent, int bucketIndex) {
                return (parent == null) ? null : mbrOnFlyBucketCache.getObject(mofBucketParentIdBucketIndexKeyDef, mofBucketParentIdBucketIndexKeyDef.createKeyFromParentIdBucketIndex(parent.getId(), bucketIndex));
            }
        };
    }

    public HspMember saveMemberOnTheFly(HspMember parent, String memberName, int sessionId) throws Exception {
        //TODO: throw an exception if parent does not support dynamic children
        HspMember member = createDynamicChildForParent(parent, memberName, null);
        //        try {
        saveMember(member, DynamicChildStrategy.ALWAYS_DYNAMIC_IF_ENABLED, sessionId);
        //        } catch (Exception e) {
        //            e.printStackTrace();
        //            throw e;
        //            //throw new RuntimeException("Failed to add member on the fly ..."); // TODO fix this message
        //        }
        return member;
    }

    public HspMember createDynamicChildForParent(HspMember parent, String memberName, UUID uuid) {
        HspMember member = (HspMember)parent.cloneForUpdate();
        // Set of MOF UUID, goes into JAPI XML file but not to Planning RDB !
        member.setUuid((uuid == null) ? HspMember.getNewUuid() : uuid);
        member.setParent(parent);
        member.setRemovable(1);
        member.setParentId(parent.getId());
        member.setChildren(null);
        member.setRemovable(true);
        if (memberName != null)
            member.setObjectName(memberName);
        member.setOldName(null);
        member.setOldUniqueName(null);
        member.setPosition(HspConstants.kMemberPositionLastSibling);
        member.setConsolOp(HspConstants.PLAN_TYPE_ALL, HspConstants.kDataConsolAddition);
        member.setDataStorage(HspConstants.kDataStorageStoreData);
        member.setIdForCache(-1);
        member.setPsMemberId(-1);
        member.setBaseMemberId(-1);
        member.setMemberOnFlyDetailToBeSaved(null);


        return member;
    }

    public void associateBaseDimensions(int planType, int sessionId) throws Exception {
        hspStateMgr.verify(sessionId);
        Vector<HspDimension> baseDims = getBaseDimensions(sessionId);
        Vector<HspDimension> hiddenDims = getHiddenDimensions(HspConstants.PLAN_TYPE_ALL, sessionId);
        baseDims.addAll(hiddenDims);
        for (int i = 0; i < baseDims.size(); i++) {
            HspDimension dim = (HspDimension)baseDims.elementAt(i);
            // we are not associating Hsp_view dimension. This is taken care during cube creation based on sandboxEnabled property
            if (dim != null && dim.getDimId() <= HspConstants.kDimensionLast && dim.getDimId() != HspConstants.gObjDim_View) {
                if (hspSystemConfig.isMultiCurrency() || hspSystemConfig.isSimpleMultiCurrency() || dim.getDimId() != HspConstants.gObjDim_Currency) {
                    HspDimension newDim = (HspDimension)dim.cloneForUpdate();
                    newDim.setUsedIn(newDim.getUsedIn() | planType);
                    if (dim.isObjectLocked()) {
                        newDim.setOverrideLock(true);
                    }
                    saveDimension(newDim, sessionId);
                    newDim.setOverrideLock(false);
                }
            }
        }
    }

    public HspDimension associateDimensionToPlanType(int dimId, int planType, int sessionId) throws Exception {
        HspDimension dim = getDimRoot(dimId);
        dim = (HspDimension)dim.cloneForUpdate();
        dim.setUsedIn(dim.getUsedIn() | planType);
        if (dim.isObjectLocked()) {
            dim.setOverrideLock(true);
        }
        saveDimension(dim, sessionId);
        return dim;
    }

    public void disassociateDimensions(int planType, int sessionId) throws Exception {
        hspStateMgr.verify(sessionId);
        Vector<HspDimension> baseDims = getBaseDimensions(sessionId);

        Vector<HspDimension> hiddenDims = getHiddenDimensions(HspConstants.PLAN_TYPE_ALL, sessionId);
        if (hiddenDims != null)
            baseDims.addAll(hiddenDims);

        for (int i = 0; i < baseDims.size(); i++) {
            HspDimension dim = (HspDimension)baseDims.elementAt(i);
            if ((dim.getUsedIn() & planType) == planType) {
                HspDimension newDim = (HspDimension)dim.cloneForUpdate();
                newDim.setUsedIn(newDim.getUsedIn() & (~planType));
                saveDimension(newDim, sessionId);
            }
        }
    }

    private void disassociateDimensions(HspActionSet actionSet, int planType, int sessionId) throws Exception {
        hspStateMgr.verify(sessionId);
        Vector<HspDimension> baseDims = getBaseDimensions(sessionId);

        Vector<HspDimension> hiddenDims = getHiddenDimensions(planType, sessionId);
        if (hiddenDims != null)
            baseDims.addAll(hiddenDims);

        for (int i = 0; i < baseDims.size(); i++) {
            HspDimension dim = (HspDimension)baseDims.elementAt(i);
            if ((dim.getUsedIn() & planType) == planType) {
                HspDimension newDim = (HspDimension)dim.cloneForUpdate();
                newDim.setUsedIn(newDim.getUsedIn() & (~planType));
                saveDimension(newDim, actionSet, sessionId);
            }
        }
    }

    public void associateMemberToPlanType(HspMember member, int planType, boolean updateChildren, int sessionId) throws Exception {
        hspStateMgr.verify(sessionId);

        if (member == null)
            throw new IllegalArgumentException("Cannot update a null member");

        final HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));

        HspCube cube = getCubeByPlanType(planType);
        if (cube == null)
            throw new RuntimeException("not a valid plantype");

        //For Dimension we not need to check but otherwise we have to check if the parent is already
        //enabled for the plantype or not.
        if (!member.isRootMember()) {
            HspMember parent = getDimMember(member.getDimId(), member.getParentId());
            if ((parent.getUsedIn() & planType) != planType) {
                throw new RuntimeException("parent is not enabled for the plantype");
            }
        }
        boolean dimLocked = false;
        try {
            lockDimension(getDimRoot(member.getDimId()).getName(), sessionId);
            dimLocked = true;
            HspActionSet actionSet = new HspActionSet(hspSQL, user);
            associateMemberToPlanType(actionSet, member, planType, updateChildren);
            HspDimension dim = getDimRoot(member.getDimId());
            boolean clearDimCache = hspSQL.isOracleDatabase();
            boolean hasAttributeDims = false;
            if (member.isRootMember()) {
                if ((dim.getUsedIn() & planType) != planType) {
                    HspDimension updDim = (HspDimension)dim.cloneForUpdate();
                    updDim.setUsedIn(member.getUsedIn() | planType);
                    HspAction action = new HspDimensionUsedInAction();
                    //setting HAS_ATTRIB_DIMS as a performance fix to avoid attribute dim/members
                    //related queries to be executed if they are not required
                    if (dim.getSupportsAttributes()) {
                        hasAttributeDims = !HspUtils.isNullOrEmpty(getAttributeDimensionsForBaseDim(dim.getDimId()));
                    }
                    action.setParameter(HspDimensionUsedInAction.HAS_ATTRIB_DIMS, hasAttributeDims);
                    //set ENSURE_OPTIMISTIC_CONCURRENCY as FALSE since the Dimension object timestamp is already updated
                    //as member object otherwise due to timestamp difference this check will fail
                    action.setParameter(HspDimensionUsedInAction.ENSURE_OPTIMISTIC_CONCURRENCY, false);
                    actionSet.addAction(action, HspActionSet.UPDATE, updDim);
                    clearDimCache = false;
                }
            }
            if (clearDimCache) {
                actionSet.addAction(new HspDimensionUsedInAction(), HspActionSet.CUSTOM, dim);
            }
            actionSet.doActions();
        } finally {
            if (dimLocked)
                unlockDimension(getDimRoot(member.getDimId()).getName(), sessionId);
        }
    }

    private void associateMemberToPlanType(HspActionSet actionSet, HspMember member, int planType, boolean updateChildren) throws Exception {
        if (member == null)
            throw new IllegalArgumentException("Cannot update a null member");

        //Update SELF
        if ((member.getUsedIn() & planType) != planType) {
            HspMember updMem = (HspMember)member.cloneForUpdate();
            updMem.setUsedIn(member.getUsedIn() | planType);

            if (updateChildren && hspSQL.isOracleDatabase()) {
                HspAction action = new HspMemberUsedInAction();
                action.setParameter(HspMemberUsedInAction.PLAN_TYPE, planType);
                //For Root member, do not do anything extra for shared members as they will be automatically processed
                //like normal members
                action.setParameter(HspMemberUsedInAction.HANDLE_SHARED_MEMBERS, !member.isRootMember());
                actionSet.addAction(action, HspActionSet.ADD, updMem);
                //TODO - handle shared members
                //TODO - member formula impact(for delete)
                //TODO - any other property impact
            } else {
                actionSet.addAction(new HspMemberUsedInAction(), HspActionSet.UPDATE, updMem);
            }
        }
        //Update childrens
        if (updateChildren && !hspSQL.isOracleDatabase() && member.hasChildren()) {
            Vector<HspMember> childrens = member.getChildren();
            if (childrens != null) {
                for (HspMember child : childrens) {
                    if (child != null) {
                        associateMemberToPlanType(actionSet, child, planType, updateChildren);
                    }
                }
            }
        }
    }

    public void disassociateMemberToPlanType(HspMember member, int planType, int sessionId) throws Exception {
        hspStateMgr.verify(sessionId);

        if (member == null)
            throw new IllegalArgumentException("Cannot update a null member");

        final HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));

        HspCube cube = getCubeByPlanType(planType);
        if (cube == null)
            throw new RuntimeException("not a valid plantype");

        boolean dimLocked = false;
        try {
            lockDimension(getDimRoot(member.getDimId()).getName(), sessionId);
            dimLocked = true;
            HspActionSet actionSet = new HspActionSet(hspSQL, user);
            disassociateMemberToPlanType(actionSet, member, planType);
            HspDimension dim = getDimRoot(member.getDimId());
            boolean clearDimCache = hspSQL.isOracleDatabase();
            boolean hasAttributeDims = false;
            if (member.isRootMember()) {
                if ((dim.getUsedIn() & planType) == planType) {
                    HspDimension updDim = (HspDimension)dim.cloneForUpdate();
                    updDim.setUsedIn(dim.getUsedIn() & (~planType));
                    HspAction action = new HspDimensionUsedInAction();
                    //setting HAS_ATTRIB_DIMS as a performance fix to avoid attribute dim/members
                    //relatd queries to be executed if they are not required
                    if (dim.getSupportsAttributes()) {
                        hasAttributeDims = !HspUtils.isNullOrEmpty(getAttributeDimensionsForBaseDim(dim.getDimId()));
                    }
                    action.setParameter(HspDimensionUsedInAction.HAS_ATTRIB_DIMS, hasAttributeDims);
                    //set ENSURE_OPTIMISTIC_CONCURRENCY as FALSE since the Dimension object timestamp is already updated
                    //as member object otherwise due to timestamp difference this check will fail
                    action.setParameter(HspDimensionUsedInAction.ENSURE_OPTIMISTIC_CONCURRENCY, false);
                    actionSet.addAction(action, HspActionSet.UPDATE, updDim);
                    clearDimCache = false;
                }
            }
            if (clearDimCache) {
                actionSet.addAction(new HspDimensionUsedInAction(), HspActionSet.CUSTOM, dim);
            }
            actionSet.doActions();
        } finally {
            if (dimLocked)
                unlockDimension(getDimRoot(member.getDimId()).getName(), sessionId);
        }
    }

    private void disassociateMemberToPlanType(HspActionSet actionSet, HspMember member, int planType) throws Exception {
        if (member == null)
            throw new IllegalArgumentException("Cannot update a null member");

        //Update SELF
        if ((member.getUsedIn() & planType) == planType) {
            HspMember updMem = (HspMember)member.cloneForUpdate();
            updMem.setUsedIn(member.getUsedIn() & (~planType));
            if (hspSQL.isOracleDatabase()) {
                HspAction action = new HspMemberUsedInAction();
                action.setParameter(HspMemberUsedInAction.PLAN_TYPE, planType);
                //For Root member, do not do anything extra for shared members as they will be automatically processed
                //like normal members
                action.setParameter(HspMemberUsedInAction.HANDLE_SHARED_MEMBERS, !member.isRootMember());
                actionSet.addAction(action, HspActionSet.DELETE, updMem);
                //TODO - handle shared members
            } else {
                actionSet.addAction(new HspMemberUsedInAction(), HspActionSet.UPDATE, updMem);
            }
        }

        //Update childrens
        if (!hspSQL.isOracleDatabase() && member.hasChildren()) {
            Vector<HspMember> childrens = member.getChildren();
            if (childrens != null) {
                for (HspMember child : childrens) {
                    if (child != null) {
                        disassociateMemberToPlanType(actionSet, child, planType);
                    }
                }
            }
        }
    }

    public Vector<HspDimension> getDimensionEvaluationOrder(int planType, int sessionId) {
        hspStateMgr.verify(sessionId);
        Vector<HspDimension> dims = getBaseDimensions(planType, sessionId);
        Vector<HspDimension> enumDims = new Vector<HspDimension>();
        if (dims != null) {
            // only include dims that have evaluation order set
            for (HspDimension d : dims) {
                if (d.getEnumOrder(planType) > 0)
                    enumDims.add(d);
            }
        }
        try {
            if (enumDims.size() > 0)
                HspUtils.sortVector(enumDims, new DimensionEnumEvalOrderComparator(planType));
        } catch (Exception e) {
            logger.finer("Exception caught while sorting dimension enum evaluation order");
        }

        return enumDims;
    }

    public boolean isPlanningCubeByCubeName(int essbaseServerId, String appName, String cubeName) throws Exception {
        return isPlanningCube(essbaseServerId, appName, cubeName, true);
    }

    public boolean isPlanningCubeByPlanTypeName(int essbaseServerId, String appName, String planTypeName) throws Exception {
        return isPlanningCube(essbaseServerId, appName, planTypeName, false);
    }

    private boolean isPlanningCube(int essbaseServerId, String appName, String cubeNameOrPlanTypeName, boolean providedCubeName) throws Exception {
        //Checks whether given essbase details are for planning cube
        boolean isPlanningCube = false;
        if (essbaseServerId == HspExternalServer.DEFAULT_ESSBASE_SERVER_ID) {
            boolean reportingAppIsCurrentAppForASO = false;
            boolean reportingAppIsCurrentAppForBSO = hspJS.getAppName().equals(appName);
            if (!reportingAppIsCurrentAppForBSO) {
                HspCube planningCube = null;
                if (providedCubeName)
                    planningCube = getCubeByCubeName(cubeNameOrPlanTypeName);
                else
                    planningCube = getCubeByPlanTypeName(cubeNameOrPlanTypeName);
                if (planningCube != null && planningCube.getAppName() != null && planningCube.getAppName().equalsIgnoreCase(appName)) {
                    reportingAppIsCurrentAppForASO = true;
                }
            }
            isPlanningCube = reportingAppIsCurrentAppForBSO || reportingAppIsCurrentAppForASO;

        }
        return isPlanningCube;
    }

    public boolean isDpBrMapping(HspCubeLink cubelink) throws Exception {
        boolean isDpBrMapping = false;
        if (cubelink != null) {
            if (isPlanningCubeByCubeName(cubelink.getTgtServerId(), cubelink.getReportingApp(), cubelink.getReportingCube())) {
                isDpBrMapping = isDpBrCube(cubelink);
            }
        }
        return isDpBrMapping;
    }


    private boolean isDpBrCube(HspCubeLink cubelink) throws Exception {
        boolean isDpBrCube = false;
        List<HspDimension> sourceCubeDimensions = getBaseDimensions(cubelink.getPlanType(), internalSessionId);
        boolean isSourcePlanTypeHasRequest = false;
        boolean isSourcePlanTypeHasDpOrBr = false;
        for (HspDimension dim : sourceCubeDimensions) {
            if (dim.getDimId() == HspConstants.kDimensionBudgetRequest)
                isSourcePlanTypeHasRequest = true;
            else if (dim.getDimId() == HspConstants.kDimensionBudgetRequestsASO || dim.getDimId() == HspConstants.kDimensionDecisionPackagesASO) {
                isSourcePlanTypeHasDpOrBr = true;
                break;
            }
        }
        if ((!isSourcePlanTypeHasDpOrBr) && isSourcePlanTypeHasRequest) {
            HspCube reportingCube = getCube(cubelink.getReportingCube());
            List<HspDimension> reportingCubeDimensions = getBaseDimensions(reportingCube.getPlanType(), internalSessionId);
            boolean isReportingPlanTypeHasDP = false;
            boolean isReportingPlanTypeHasBR = false;
            boolean isReportingPlanTypeHasRequest = false;
            for (HspDimension dim : reportingCubeDimensions) {
                if (dim.getDimId() == HspConstants.kDimensionDecisionPackagesASO)
                    isReportingPlanTypeHasDP = true;
                else if (dim.getDimId() == HspConstants.kDimensionBudgetRequestsASO)
                    isReportingPlanTypeHasBR = true;
                else if (dim.getDimId() == HspConstants.kDimensionBudgetRequest) {
                    isReportingPlanTypeHasRequest = true;
                    break;
                }
            }
            if ((!isReportingPlanTypeHasRequest) && isReportingPlanTypeHasDP && isReportingPlanTypeHasBR) {
                isDpBrCube = true;
            }
        }
        return isDpBrCube;
    }

    public HspDPMember getBRForEssbaseMember(String scenario, String version, int brDimMemberId, int sessionId) {
        HspDPMember dpMember = null;
        List<HspDPMember> decisionPackages = getDecisionPackages(scenario, version, sessionId);
        for (HspDPMember dp : decisionPackages) {
            List<HspDPMember> budgetRequests = getBudgetRequests(scenario, version, dp.getName(), sessionId);
            if (!HspUtils.isNullOrEmpty(budgetRequests)) {
                for (HspDPMember br : budgetRequests) {
                    if (br.getBrDimMemberId() == brDimMemberId) {
                        dpMember = br;
                        break;
                    }
                }
            }
        }
        return dpMember;
    }

    public void addCubeSLMapping(HspCubeSLMapping mapping, int sessionId) throws Exception {
        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        HspCubeSLMappingAction cubeSLMappingAction = new HspCubeSLMappingAction();
        actionSet.addAction(cubeSLMappingAction, HspActionSet.ADD, mapping);
        actionSet.doActions();
    }

    public void updateCubeSLMapping(HspCubeSLMapping mapping, int sessionId) {
    }

    public void deleteCubeSLMapping(HspCubeSLMapping mapping, int sessionId) {
    }

    public synchronized void addDPCubeMapping(HspDPCubeMapping mapping, int sessionId) throws Exception {
        if (mapping == null) // check
            throw new RuntimeException("Invalid mapping: null");

        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        HspDPCubeMappingAction dpCubeMappingAction = new HspDPCubeMappingAction();
        actionSet.addAction(dpCubeMappingAction, HspActionSet.ADD, mapping);
        actionSet.doActions();
    }

    public void updateDPCubeMapping(HspDPCubeMapping mapping, int sessionId) throws Exception {
        if (mapping == null) // check
            throw new RuntimeException("Invalid mapping: null");

        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        HspDPCubeMappingAction dpCubeMappingAction = new HspDPCubeMappingAction();
        actionSet.addAction(dpCubeMappingAction, HspActionSet.UPDATE, mapping);
        actionSet.doActions();
    }

    public synchronized void deleteDPCubeMapping(HspDPCubeMapping mapping, int sessionId) throws Exception {
        if (mapping == null) // check
            throw new RuntimeException("Invalid mapping: null");

        HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
        HspActionSet actionSet = new HspActionSet(hspSQL, user);
        HspDPCubeMappingAction dpCubeMappingAction = new HspDPCubeMappingAction();
        actionSet.addAction(dpCubeMappingAction, HspActionSet.DELETE, mapping);
        actionSet.doActions();
    }

    private boolean isNewDimension(HspDimension dimension, int sessionId) throws Exception {
        HspDimension oldDimension = getDimRoot(dimension.getDimId());
        boolean weAreAdding = true;
        if (dimension.getDimId() == HspConstants.kDimensionRates) {
            weAreAdding = false;
        } else {
            weAreAdding = (oldDimension == null);
            if (weAreAdding && getNumDimensions(sessionId) >= HspConstants.MAX_NUM_DIMENSIONS) {
                if (isDimensionCounted(dimension))
                    throw new HspRuntimeException("MSG_MAX_DIMS_REACHED");
            }
        }
        return weAreAdding;
    }

    private boolean isDimensionCounted(HspDimension dimension) {
        HspDimension oldDimension = getDimRoot(dimension.getDimId());
        boolean countDimension = true;
        if (dimension.getDimId() == HspConstants.kDimensionRates) {
            countDimension = false;
        } else {
            countDimension = (oldDimension == null);
            if (countDimension) {
                if (dimension.getActualObjectType() == HspConstants.gObjType_AttributeDim || dimension.getActualObjectType() == HspConstants.gObjType_PMDimension || dimension.getActualObjectType() == HspConstants.gObjType_DPDimension ||
                    dimension.getActualObjectType() == HspConstants.gObjType_DecisionPackageASO || dimension.getActualObjectType() == HspConstants.gObjType_BudgetRequestASO || dimension.getActualObjectType() == HspConstants.gObjType_ExternalMember)
                    countDimension = false;
            }
        }
        return countDimension;
    }


    private void updateDimensionPostSave(HspDimension dimension, int sessionId) throws Exception {
        if (hspJS.getSystemCfg().isSimpleMultiCurrency()) {
            if (!dimension.isVirtualDimension()) {
                //Bug 22529212  - skipping add "No Member" for attribute dimension
                if (dimension.getObjectType() == HspConstants.gObjType_AttributeDim || dimension.getObjectType() == HspConstants.gObjType_AttributeMember)
                    return;
                addSimpleCurrNoMember(dimension, sessionId);
                updateXchgeRateForm(dimension, sessionId, true);
            }
        }
    }

    private void addSimpleCurrNoMember(HspDimension dimension, int sessionId) throws Exception {
        // add No Member
        HspMember defaultMember = createMember(dimension.getId());
        defaultMember.setObjectName(HspSimpleCurrencyUtils.STR_NO_MBR + dimension.getName());
        defaultMember.setDimId(dimension.getDimId());
        defaultMember.setObjectType(HspConstants.gObjType_UserDefinedMember);
        defaultMember.setUsedIn(dimension.getUsedIn());
        defaultMember.setParentId(dimension.getDimId());
        defaultMember.setDataStorage(HspConstants.kDataStorageStoreData);
        defaultMember.setDataType(HspConstants.DATA_TYPE_UNSPECIFIED);
        //defaultMember.setRemovable(2);

        saveMember(defaultMember, sessionId);
    }

    private void updateXchgeRateForm(HspDimension dimension, int sessionId, boolean weAreAdding) throws Exception {
        int planType = 0;

        if (hspJS.getSystemCfg().isSimpleMultiCurrency()) {
            HspForm exchangeRateForm = hspFMDB.getExchangeRateForm(sessionId);
            if (exchangeRateForm != null) {
                HspFormDef formdef = hspFMDB.getFormDefForEditing(exchangeRateForm.getId(), sessionId);
                if (formdef != null) {
                    planType = formdef.getBaseForm().getPlanType();

                    if (weAreAdding) {
                        if (planType == (dimension.getUsedIn() & planType)) {
                            formdef.addDimension(HspConstants.POV, dimension.getId());
                            formdef.getBaseForm().setOverrideLock(true);
                            hspFMDB.updateForm(formdef, sessionId);

                            // Bug 25089880 - GETTING "SQL OPERATION FAILED" ERROR WHILE ENABLING CUBE FOR A DIMENSION
                            // Get the member using getDimMember instead of getMemberByName since it is more efficient
                            // and supports dimension with usedIn = 0
                            HspMember mbr = getDimMember(dimension.getId(), HspSimpleCurrencyUtils.STR_NO_MBR + dimension.getName());
                            // If the "none" member exist use it otherwise use the root member
                            // TODO: EPBCS team to decide if a localized exception should be thrown instead of adding root member
                            if (mbr == null)
                                mbr = getDimMember(dimension.getId(), dimension.getId());
                            Vector memberList = new Vector();
                            memberList.add(new HspFormMember(mbr));
                            formdef.addMembers(HspConstants.DEF_LOC, dimension.getId(), memberList);
                            hspFMDB.updateForm(formdef, sessionId);
                            return;
                        }
                    } else {
                        if ((dimension.getLastSavedUsedIn() & planType) == planType && (dimension.getUsedIn() & planType) == 0) {
                            //update exchange rate form
                            formdef.getBaseForm().setOverrideLock(true);
                            formdef.clearMembers(HspConstants.DEF_LOC, dimension.getDimId());
                            hspFMDB.updateForm(formdef, sessionId);
                        }
                        if ((dimension.getLastSavedUsedIn() & planType) == 0 && (dimension.getUsedIn() & planType) == planType) {
                            updateXchgeRateForm(dimension, sessionId, true);
                        }
                    }
                }
            }
        }
    }

    private synchronized void saveReportingCurrency(HspMember member, int sessionId) throws Exception {

        HspCurrency rptMem = (HspCurrency)getMemberByName(member.getName() + HspSimpleCurrencyUtils.STR_REPORTING);
        boolean isFccs = hspJS.getSystemCfg().isFCCSApp();
        boolean isTrcs = hspJS.getSystemCfg().isTrcsApp();
        if (rptMem == null && ((HspCurrency)member).isReportingCurrency()) {
            HspCurrency rptMember = (HspCurrency)createMember(HspConstants.kDimensionSimpleCurrency);
            rptMember.setParentId(HspSimpleCurrencyUtils.MEMBER_ID_HSP_REPORTING_CURRENCIES);

            //this is done to override parent lock so that we can add child to it
            HspMember parent = getMemberById(HspSimpleCurrencyUtils.MEMBER_ID_HSP_REPORTING_CURRENCIES);
            HspMember parentCopy = (HspMember)parent.cloneForUpdate();
            parentCopy.setOverrideLock(true);
            rptMember.setParent(parentCopy);

            rptMember.setObjectType(HspConstants.gObjType_SimpleCurrency);
            rptMember.setObjectName(member.getName() + HspSimpleCurrencyUtils.STR_REPORTING);
            //Temporary Fix for bug 23022283
            //TODO Need to changee the Insert query which does not insert Description. Should be taken care in Monthly patch.
            //rptMember.setDescription(member.getDescription());

            rptMember.setThousandsSeparator(((HspCurrency)member).getThousandsSeparator());
            rptMember.setDecimalSeparator(((HspCurrency)member).getDecimalSeparator());
            rptMember.setNegativeColor(((HspCurrency)member).getNegativeColor());
            rptMember.setNegativeStyle(((HspCurrency)member).getNegativeStyle());
            rptMember.setScale(((HspCurrency)member).getScale());
            rptMember.setSymbol(((HspCurrency)member).getSymbol());
            rptMember.setReportingCurrency(((HspCurrency)member).isReportingCurrency());
            rptMember.setTriangulationCurrencyID(((HspCurrency)member).getTriangulationCurrencyID());
            rptMember.setCurrencyType(HspConstants.gintCurTypeUserDefined);
            rptMember.setPrecision(((HspCurrency)member).getPrecision());

            rptMember.setConsolOp(HspConstants.PLAN_TYPE_ALL, HspConstants.kDataConsolIgnore);
            rptMember.setUsedIn(member.getUsedIn());
            rptMember.setPosition(HspConstants.kMemberPositionLastSibling);
            //data storage
            rptMember.setDataStorage(HspConstants.kDataStorageStoreData);
            if (isFccs || isTrcs) {
                //adding alias
                Object[][] aliases = new Object[1][2];
                int aliasTableId = HspConstants.gFolder_AliasDefault;
                String alias = member.getName();
                aliases[0][0] = aliasTableId;
                aliases[0][1] = alias;
                rptMember.setShouldSaveAlias(true);
                rptMember.setAliasesToBeSaved(aliases);
            }
            //data Type
            rptMember.setDataType(member.getDataType());
            //two pass calc
            rptMember.setTwopassCalc(false);
            //not editable no adddition of children
            //rptMember.setRemovable(HspConstants.NOT_ALLOW_CHILDREN_NON_EDITABLE_OBJ_LOCK);
            rptMember.setRemovable(HspSimpleCurrencyUtils.REP_CURRENCY_OBJ_REMOVABLE);
            rptMember.setObjLockOpts(HspSimpleCurrencyUtils.REP_CURRENCY_OBJ_LOCK_OPTS);
            rptMember.setMemLockOpts(HspSimpleCurrencyUtils.REP_CURRENCY_MEM_LOCK_OPTS);

            saveMember(rptMember, DynamicChildStrategy.DYNAMIC_IF_AVAILABLE, sessionId);
        } else if (rptMem != null && ((HspCurrency)member).isReportingCurrency()) {
            //clone for update
            rptMem = (HspCurrency)rptMem.cloneForUpdate();
            //Temporary Fix for bug 23022283
            //TODO Need to changee the Insert query which does not insert Description. Should be taken care in Monthly patch.
            //rptMem.setDescription(member.getDescription());
            rptMem.setOverrideLock(true);

            rptMem.setThousandsSeparator(((HspCurrency)member).getThousandsSeparator());
            rptMem.setDecimalSeparator(((HspCurrency)member).getDecimalSeparator());
            rptMem.setNegativeColor(((HspCurrency)member).getNegativeColor());
            rptMem.setNegativeStyle(((HspCurrency)member).getNegativeStyle());
            rptMem.setScale(((HspCurrency)member).getScale());
            rptMem.setSymbol(((HspCurrency)member).getSymbol());
            rptMem.setPrecision(((HspCurrency)member).getPrecision());
            rptMem.setReportingCurrency(((HspCurrency)member).isReportingCurrency());
            rptMem.setTriangulationCurrencyID(((HspCurrency)member).getTriangulationCurrencyID());

            rptMem.setUsedIn(member.getUsedIn());
            //data Type
            rptMem.setDataType(member.getDataType());
            rptMem.setRemovable(HspSimpleCurrencyUtils.REP_CURRENCY_OBJ_REMOVABLE);
            rptMem.setObjLockOpts(HspSimpleCurrencyUtils.REP_CURRENCY_OBJ_LOCK_OPTS);
            rptMem.setMemLockOpts(HspSimpleCurrencyUtils.REP_CURRENCY_MEM_LOCK_OPTS);
            if (isFccs || isTrcs) {
                rptMem.setDataStorage(HspConstants.kDataStorageStoreData);
                rptMem.setTwopassCalc(false);
                //adding alias
                Object[][] aliases = new Object[1][2];
                int aliasTableId = HspConstants.gFolder_AliasDefault;
                String alias = member.getName();
                aliases[0][0] = aliasTableId;
                aliases[0][1] = alias;
                rptMem.setShouldSaveAlias(true);
                rptMem.setAliasesToBeSaved(aliases);
            }
            saveMember(rptMem, DynamicChildStrategy.DYNAMIC_IF_AVAILABLE, sessionId);
        } else if (rptMem != null && !((HspCurrency)member).isReportingCurrency()) {
            HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
            HspActionSet actionSet = new HspActionSet(hspSQL, user);
            deleteReportingCurrency(member.getDimId(), member.getId(), actionSet, sessionId);
            actionSet.doActions();
        }
    }

    private synchronized void deleteReportingCurrency(int dimensionId, int id, HspActionSet actionSet, int sessionId) {
        HspMember member = getDimMember(dimensionId, id);
        HspMember rptMem = getMemberByName(member.getMemberName() + HspSimpleCurrencyUtils.STR_REPORTING);
        if (rptMem != null) {
            HspAction action = new HspCurrencyAction();
            rptMem = (HspMember)rptMem.cloneForUpdate();
            rptMem.setOverrideLock(true);
            deleteMemberAliases(actionSet, rptMem, null, sessionId);
            actionSet.addAction(action, HspActionSet.DELETE, rptMem);
        }

    }

    public HspEssApplication getReportingApp(HspExternalServer reportingCubeServer, String appName, int sessionId) throws Exception {
        logger.entering();
        try {
            hspStateMgr.verify(sessionId);
            return isPlanningPodServer(reportingCubeServer, sessionId) ? HspRestObjectHelper.getEssApplication(hspRepDimMbrCacheMgr, reportingCubeServer, appName) : HspEssbaseObjectHelper.getEssApplication(hspOLAP, reportingCubeServer, appName);
        } finally {
            logger.exiting();
        }
    }


    public HspEssCube getReportingCube(HspExternalServer reportingCubeServer, String reportingAppName, String cubeName, int sessionId) throws Exception {
        logger.entering();
        try {
            hspStateMgr.verify(sessionId);
            return isPlanningPodServer(reportingCubeServer, sessionId) ? HspRestObjectHelper.getEssCube(hspRepDimMbrCacheMgr, reportingCubeServer, reportingAppName, cubeName) : HspEssbaseObjectHelper.getEssCube(hspOLAP, reportingCubeServer, reportingAppName, cubeName);
        } finally {
            logger.exiting();
        }
    }

    public Vector<HspEssApplication> getReportingApps(HspExternalServer reportingCubeServer, int sessionId) throws Exception {
        logger.entering();
        try {
            hspStateMgr.verify(sessionId);
            return isPlanningPodServer(reportingCubeServer, sessionId) ? HspRestObjectHelper.getEssApplications(hspRepDimMbrCacheMgr, reportingCubeServer) : HspEssbaseObjectHelper.getEssApplications(hspOLAP, reportingCubeServer);
        } finally {
            logger.exiting();
        }
    }

    public Vector<HspEssCube> getReportingCubes(HspExternalServer reportingCubeServer, String reportingAppName, int sessionId) throws Exception {
        logger.entering();
        try {
            hspStateMgr.verify(sessionId);
            return isPlanningPodServer(reportingCubeServer, sessionId) ? HspRestObjectHelper.getEssCubes(hspRepDimMbrCacheMgr, reportingCubeServer, reportingAppName) : HspEssbaseObjectHelper.getEssCubes(hspOLAP, reportingCubeServer, reportingAppName);
        } finally {
            logger.exiting();
        }
    }

    public boolean isPlanningPodServer(HspExternalServer reportingCubeServer, int sessionId) throws Exception {
        logger.entering();
        try {
            hspStateMgr.verify(sessionId);
            //            return (reportingCubeServer.getType() == HspExternalServer.SERVER_TYPE_PLANNING_POD);
            return reportingCubeServer.getServer().startsWith("http://") || reportingCubeServer.getServer().startsWith("https://");
        } finally {
            logger.exiting();
        }
    }

    public List<HspExternalServer> getPlanningAppConnections(int sessionId) throws Exception {
        List<HspExternalServer> planningAppConnections = new ArrayList<HspExternalServer>();
        List<HspExternalServer> externalServers = new ArrayList<HspExternalServer>();
        externalServers = getEssbaseServers(sessionId);
        for (HspExternalServer externalServer : externalServers) {
            if (externalServer.getServerId() == HspExternalServer.DEFAULT_ESSBASE_SERVER_ID || externalServer.getType() == HspExternalServer.SERVER_TYPE_PLANNING_POD) {
                planningAppConnections.add(externalServer);
            }
        }
        return planningAppConnections;
    }

    public HspMember getSharedMember(String objectName, String parentObjectName, int sessionId) {
        logger.entering();
        return getSharedMember(objectName, getMemberByName(parentObjectName), sessionId);
    }

    private HspMember getSharedMember(String objectName, HspMember parent, int sessionId) {
        hspStateMgr.verify(sessionId);
        Vector<HspMember> childMems = parent == null ? null : getChildMembers(parent.getDimId(), parent.getId(), sessionId);
        if (childMems != null) {
            for (HspMember member : childMems) {
                if (member != null && member.getObjectName().equals(objectName)) {
                    return member;
                }
            }
        }
        return null;
    }

    public HspMember getDimSharedMember(int dimId, String objectName, String parentObjectName, int sessionId) {
        logger.entering();
        return getSharedMember(objectName, getDimMember(dimId, parentObjectName), sessionId);
    }

    private void updatePlanTypeLockOpts() {
        Vector<HspDimension> dimensions = dimensionCache.getUnfilteredCache();
        for (int loop1 = 0; loop1 < dimensions.size(); loop1++) {
            HspDimension dim = dimensions.elementAt(loop1);
            if (dim != null) {
                Vector<HspCube> props = cubeCache.getUnfilteredCache();
                Map<Integer, Integer> ptLockOptsMap = new HashMap<Integer, Integer>();
                for (HspCube cube : props) {
                    ptLockOptsMap.put(cube.getPlanType(), cube.getPtLockOpts());
                }
                dim.setPlanTypeLockOpts(ptLockOptsMap);
            }
        }
    }

    public HspAttributeMember getAttributeMemberByReferenceId(int attrDimId, int referenceId) {
        GenericCache<HspAttributeMember> memberCache = getMembersCache(attrDimId);
        if (memberCache != null) {
            return memberCache.getObject(attrMemberRefKeyDef, attrMemberRefKeyDef.createKeyFromAttribMemberReferenceId(referenceId));
        }
        return null;
    }

    public void syncReferenceAttributeMembersForAttrDim(int attrDimId, int sessionId) {
        HspAttributeDimension attrDim = getAttributeDimension(attrDimId);
        syncReferenceAttributeMembersForAttrDim(attrDim, sessionId);
    }

    public void syncReferenceAttributeMembersForAttrDim(String attrDimName, int sessionId) {
        HspAttributeDimension attrDim = getAttributeDimension(attrDimName);
        syncReferenceAttributeMembersForAttrDim(attrDim, sessionId);
    }

    public void syncReferenceAttributeMembersForAttrDim(HspAttributeDimension attrDim, int sessionId) {
        //HspAttributeDimension attrDim = getAttributeDimension(attrDimId);
        if (attrDim == null)
            throw new IllegalArgumentException("Attribute dimension cannot be null");

        if (attrDim.getReferenceDimId() > 0) {
            HspDimension referenceDim = getDimRoot(attrDim.getReferenceDimId());
            if (referenceDim == null)
                throw new IllegalArgumentException("Could not retrieve reference dimension for attribute dimension, Reference dimension ID: " + attrDim.getReferenceDimId());

            HspUser user = hspSecDB.getUser(hspStateMgr.getUserId(sessionId));
            HspActionSet actionSet = new HspActionSet(hspSQL, user);

            HspMbrSelection mbrSelection = attrDim.getMbrSelection();
            Vector<HspMember> existingAttrDimMbrs = getDimMembers(attrDim.getId(), false, sessionId);
            HspActionSetFiller asf = new HspActionSetFiller(actionSet);
            Vector<HspMember> refDimAttrMbrs = new Vector<HspMember>();
            List<TriVal<Integer, HspMember, HspMember>> result = null;

            try {
                if (mbrSelection != null) {
                    // process list from evaluated member selection result
                    Vector<HspCube> cubes = getCubes(sessionId);
                    List<HspFormCell> mergedResult = new Vector<HspFormCell>();
                    for (int i = 1; i < Math.pow(2, cubes.size()); i = i * 2) {
                        Vector<HspFormCell> selectedMbrs = HspRTPUtils.getEvaluatedMembersVectorFromString(mbrSelection.getRequiredDimId(), i, mbrSelection.toString(), hspJS, sessionId);
                        mergedResult = HspUtils.mergeListsWithOr(mergedResult, selectedMbrs);
                    }
                    // Remove duplicate member names from list
                    Vector<HspFormCell> filteredDimMbrs = new Vector<HspFormCell>();
                    CollectionUtils.select(mergedResult, HspPredicateUtils.distinctPredicate(new HspFormMemberNameComparator()), filteredDimMbrs);
                    CollectionUtils.collect(filteredDimMbrs, HspTransformerUtils.formMemberToReferenceAttributeMemberTransformer(attrDim, this), refDimAttrMbrs);
                } else {
                    Vector<HspMember> selectedMbrs = getDimMembers(attrDim.getReferenceDimId(), false, sessionId);
                    Vector<HspMember> filteredDimMbrs = new Vector<HspMember>();
                    // Remove duplicate member names from list
                    CollectionUtils.select(selectedMbrs, HspPredicateUtils.distinctPredicate(new HspMemberNameComparator()), filteredDimMbrs);
                    CollectionUtils.collect(filteredDimMbrs, HspTransformerUtils.memberToReferenceAttributeMemberTransformer(attrDim, this), refDimAttrMbrs);
                }

                result = HspUtils.getMergeActions(existingAttrDimMbrs, refDimAttrMbrs, attrMemberRefKeyDef);
                if (result != null) {
                    asf.addChangeList(new HspAttributeMemberAction(), result);
                    asf.fill();
                    actionSet.doActions();
                }
            } catch (Exception e) {
                Properties props = new Properties();
                props.put("DIM_NAME", attrDim.getObjectName());
                throw new HspRuntimeException("MSG_FAILED_TO_SYNC_ATTRIBUTE_DIM", props, e);
            }
        } else {
            logger.fine("Reference dimension was 0 so nothing to sync");
        }

    }

    //22994680 - in order to skip validation for binding non-indexed attributes with a shared/label-only member

    private boolean containsIndexedAttributes(HspAttributeMemberBinding[] attrBindings) {
        if (attrBindings == null || attrBindings.length == 0)
            return false;
        for (HspAttributeMemberBinding attrBinding : attrBindings) {
            HspAttributeDimension attrDim = getAttributeDimension(attrBinding.getAttributeDimId());
            if (attrDim != null && attrDim.isIndexed())
                return true;
        }
        return false;
    }

    /**
     * Updates No Member of custom dimension with dimension usedIn
     * Bug Fix 23346894
     * @param dimension
     * @param actionSet
     * @param sessionId
     * @throws Exception
     */
    private void updateNoMemberForSimplifiedCurrency(HspDimension dimension, HspActionSet actionSet, int sessionId) throws Exception {

        if (dimension.getUsedIn() != dimension.getLastSavedUsedIn() && hspJS.getSystemCfg().isSimpleMultiCurrency() && (dimension.getObjectType() == HspConstants.gObjType_UserDefinedMember || dimension.getObjectType() == HspConstants.gObjType_Entity)) {

            HspMember member = getMemberByName(HspSimpleCurrencyUtils.STR_NO_MBR + ((dimension.getObjectType() == HspConstants.gObjType_Entity) ? HspSimpleCurrencyUtils.ENTITY_DIM_NAME : dimension.getName()));
            updateNoMemberAndParentForSimplifiedCurrency(dimension, member, actionSet, sessionId);
        }
    }

    /**
     * Set UsedIn of No Member and its parent hierarchy to dimension UsedIn
     * Adding member to the actionset if not already present else updating already exsisting one
     * Adding same object to ActionSet gives concurrency exception
     *
     * @param dimension
     * @param member
     * @param actionSet
     * @param sessionId
     * @throws Exception
     */
    private void updateNoMemberAndParentForSimplifiedCurrency(HspDimension dimension, HspMember member, HspActionSet actionSet, int sessionId) throws Exception {
        if (member != null && !member.isRootMember()) {
            HspMember parent = (HspMember)member.getParent();
            updateNoMemberAndParentForSimplifiedCurrency(dimension, parent, actionSet, sessionId);
            if (actionSet.getCachedObject(member) == null) {
                member = (HspMember)member.cloneForUpdate();
                member.setUsedIn(dimension.getUsedIn());
                if (member.isObjectLocked()) {
                    member.setOverrideLock(true);
                }

                //If seeded member & the user is not module user, track the changes
                boolean isObjectSeeded = hspJS.getFeatureDB(sessionId).isObjectSeeded(member.getId(), member.getObjectType());
                boolean shouldTrack = HspModuleInfo.getModuleUser() == null || (!HspModuleInfo.getModuleUser().equals(HspModuleInfo.MODULE_USER));
                List<HspDiff> diffList = null;
                if (isObjectSeeded && shouldTrack) {
                    ReadOnlyCachedObject objInCache = hspJS.getFeatureDB(sessionId).getPlanningObject(member.getObjectName(), member.getObjectType(), sessionId, member.getParentId());
                    diffList = member.getDiffList(hspJS, objInCache, sessionId);

                    //Below condition is satisfied for Shared Members for which UsedIn has been changed i.e. diffList will NOT be EMPTY
                    if (member.isSharedMember() && diffList != null && !diffList.isEmpty()) {
                        HspDiffUtil.addParameterToDiffList("PARENT_NAME", member.getParent().getName(), diffList);
                    }
                }

                updateMember(member, actionSet, null, sessionId);

                if (isObjectSeeded && shouldTrack && diffList != null && diffList.size() > 0) {
                    int artifactId = hspJS.getFeatureDB(sessionId).getModuleArtifactDetailByObjId(member.getId(), member.getObjectType()).getId();
                    // TODO: Verify the next line handles the rollback of the actionSet as this method maybe retried multiple times
                    hspJS.getFeatureDB(sessionId).addAuditRecord(HspActionSet.UPDATE, diffList, artifactId, sessionId);
                }
            } else {
                member = (HspMember)actionSet.getCachedObject(member);
                if (member.isObjectLocked()) {
                    member.setOverrideLock(true);
                }
                member.setUsedIn(dimension.getUsedIn());
            }
        }

    }

    /**
     * Methods updates version members to newly added BSO plan type and for ASO plan types
     *
     * @param dimension
     * @param actionSet
     * @param sessionId
     * @throws Exception
     */
    private void updateVersionMembersForUpdatedPlanType(HspDimension dimension, HspActionSet actionSet, int sessionId) throws Exception {
        //if new plan type is added to version dimension then version members are also updated
        if (dimension.getId() == HspConstants.gObjDim_Version && dimension.getLastSavedUsedIn() != dimension.getUsedIn() && (dimension.getUsedIn() & dimension.getLastSavedUsedIn()) == dimension.getLastSavedUsedIn()) {
            int planType = dimension.getUsedIn() ^ dimension.getLastSavedUsedIn();
            boolean isSandboxEnabled = hspJS.getSystemCfg().isSandboxEnabled(planType);

            HspMember parent = getDimMember(dimension.getDimId(), dimension.getDimId());
            Vector<HspMember> childMembers = parent.getChildren();

            if (childMembers != null) {
                for (HspMember member : childMembers) {
                    if (!isSandboxEnabled && member.getId() == HspSetupConsts.kStdBUSandboxesVersionId) {
                        continue;
                    } else {
                        Vector<HspMember> dimChildren = member.getDescendants(true, true);
                        if (dimChildren != null) {
                            for (HspMember child : dimChildren) {
                                boolean skipUpdate = false;
                                // updateMember below may also update shared member
                                // so check for shared member in actionSet to avoid
                                // duplicate entries in the actionSet
                                if (child.isSharedMember()) {
                                    HspMember shared = (HspMember)actionSet.getCachedObject(child);
                                    if (shared != null) {
                                        shared.setUsedIn(child.getUsedIn() | planType);
                                        skipUpdate = true;
                                    }
                                }
                                if (skipUpdate)
                                    continue;
                                child = (HspMember)child.cloneForUpdate();
                                child.setUsedIn(child.getUsedIn() | planType);
                                if (child.isObjectLocked()) {
                                    child.setOverrideLock(true);
                                }
                                updateMember(child, actionSet, null, sessionId);
                            }
                        }
                    }
                }
            }
        }
    }

    public String getEssbaseApplicationName(HspCube hspCube) throws Exception {
        if (hspCube == null)
            throw new IllegalArgumentException("hspCube cannot be null");

        if (hspCube.isASOCube()) {
            return hspCube.getAppName();
        } else {
            return hspJS.getAppName();
        }
    }

    /**
     * Retrieves a dimension customization object
     * used to control display and various other customizations
     * which can be applied to dimensions in the dimension editor.
     *
     * @param dimId dimension Id
     * @param sessionId
     * @return HspDimensionCustomization dimension customization member or null
     */
    public HspDimensionCustomization getHspDimensionCustomization(int dimId, int sessionId) {
        HspDimensionCustomization dimCustomization = null;
        if (hspDimensionCustomizationMgr != null) {
            dimCustomization = hspDimensionCustomizationMgr.getDimensionCustomization(dimId);
        }
        return (dimCustomization);
    }

    /**
     * Method invoked in order to force a reload from thre repository of the dimension customization objects
     */
    public void reloadDimensionCustomization(int sessionId) {
        //        int count = 0;
        //        Connection con = null;
        //
        //        // rebuild customization tables and reload them from file
        //        try{
        //             con = hspSQL.getConnection();
        //            String[] params = new String[]{};
        //             hspSQL.executeUpdate("DROP TABLE HSP_DIM_CUSTOMIZATION", con, true);
        //        }
        //        catch (Exception e)
        //        {
        //            count = 0;
        //        }
        //        finally
        //        {
        //            if (con != null)
        //                hspSQL.releaseConnection(con);
        //        }
        //
        //             try{
        //                  con = hspSQL.getConnection();
        //                 String[] params = new String[]{};
        //             if (hspSQL.isSQLServerDatabase())
        //                hspSQL.executeUpdate("CREATE TABLE HSP_DIM_CUSTOMIZATION(DIM_ID INTEGER NOT NULL, MODULE_ID INTEGER NOT NULL, CUSTOMIZATION TEXT, CONSTRAINT PK_HSP_DIM_CUSTOMIZATION PRIMARY KEY (DIM_ID, MODULE_ID))", con, true);
        //            else
        //                hspSQL.executeUpdate("CREATE TABLE HSP_DIM_CUSTOMIZATION(DIM_ID INTEGER NOT NULL, MODULE_ID INTEGER NOT NULL, CUSTOMIZATION CLOB, CONSTRAINT PK_HSP_DIM_CUSTOMIZATION PRIMARY KEY (DIM_ID, MODULE_ID))", con, true);
        //
        //
        //         }
        //         catch (Exception e)
        //         {
        //             count = 0;
        //         }
        //         finally
        //         {
        //             if (con != null)
        //                 hspSQL.releaseConnection(con);
        //         }
        //
        //        // try to retrieve all customizations from relational db
        //        try{
        //             con = hspSQL.getConnection();
        //            String[] params = new String[]{};
        //
        //             Vector v = hspSQL.executeQuery("SQL_GET_DIM_CUSTOMIZATIONS", params, con, HspDimensionCustomizationData.class);
        //         }
        //         catch (Exception e)
        //         {
        //             count = 0;
        //         }
        //         finally
        //         {
        //             if (con != null)
        //                 hspSQL.releaseConnection(con);
        //         }

        if (hspDimensionCustomizationMgr != null) {
            hspDimensionCustomizationMgr.loadDimensionCustomizations(sessionId);
        }
    }

    private void validateDimensionForSeededCubes(HspDimension dimension, int sessionId, boolean weAreAdding) throws Exception {
        HspDimension oldDimension = getDimRoot(dimension.getDimId());
        boolean fromEF = false;
        if (null != HspModuleInfo.getModuleUser()) {
            //Check whether method is invoked during Enable Features
            fromEF = HspModuleInfo.getModuleUser().equals(HspModuleInfo.MODULE_USER);
        }

        if (!fromEF && dimension.getDimType() != HspConstants.kDimTypeAttribute) {
            Vector<HspCube> cubes = getCubes(sessionId);
            if (weAreAdding) {
                //Add Dimension
                for (HspCube cube : cubes) {
                    if ((cube.getPlanType() & dimension.getUsedIn()) == cube.getPlanType()) {
                        validateSeededCube(dimension, cube);
                    }
                }
            } else {
                //Edit Dimension
                if (oldDimension.getUsedIn() != dimension.getUsedIn()) {
                    for (HspCube cube : cubes) {
                        if ((cube.getPlanType() & oldDimension.getUsedIn()) != (cube.getPlanType() & dimension.getUsedIn())) {
                            validateSeededCube(dimension, cube);
                        }
                    }
                }
            }
        }
    }

    private void validateSeededCube(HspDimension dimension, HspCube cube) throws Exception {
        //Check whether cube is NOT a GENERIC_CUBE or NOT a ASO_CUBE and a module cube
        if ((cube.getType() != HspConstants.GENERIC_CUBE) && (cube.getType() == HspConstants.ASO_CUBE && cube.getName().startsWith(HspModuleRegistry.MODULE_GLOBAL_ARTIFACT_PREFIX + "_"))) {
            Properties props = new Properties();
            props.put("DIM", dimension.getObjectName());
            props.put("CUBE", cube.getName());
            throw new HspRuntimeException("MSG_PLANTYPE_IS_SEEDED_CANNOT_ADD_DIMENSION", props);
        }
    }

    /**
     * Add currency UDA for member having currency data type
     * Delete  currency UDA binding if data type is changed from currency to other data type
     * Delete currency UDA if no more UDA bindings exists for UDA
     *
     * @param member
     * @param sessionId
     * @throws Exception
     */
    private void updateCurrencyUDAForMember(HspJS hspJs, HspMember member, int sessionId) throws Exception {

        if (hspJS.getSystemCfg().isSimpleMultiCurrency() && !(hspJS.getSystemCfg().isFCCSApp() || hspJS.getSystemCfg().isTrcsApp())) {
            //get Hsp_Currency UDA associated with dimension
            HspUDA uda = getUDA(member.getDimId(), HspConstants.UDA_HSP_CURRENCY);
            ArrayList<HspUDABinding> UDAsToBeSavedList = null;
            HspUDABinding[] UDAsToBeSaved = null;
            boolean currencyUDAExists = false;
            //check if member is associated with currency UDA
            if (uda != null) {
                Vector<HspUDABinding> udaBindings = getUDABindings(member.getId());
                if (udaBindings != null) {
                    for (int i = 0; i < udaBindings.size(); i++) {
                        HspUDABinding binding = (HspUDABinding)udaBindings.get(i);
                        if (binding.getUDAId() == uda.getId()) {
                            currencyUDAExists = true;
                            break;
                        }
                    }
                }
                UDAsToBeSaved = member.getUDAsToBeSaved();
            }
            if (UDAsToBeSaved == null) {
                UDAsToBeSavedList = new ArrayList<HspUDABinding>();
            } else {
                UDAsToBeSavedList = new ArrayList<HspUDABinding>(Arrays.asList(UDAsToBeSaved));
            }

            //add UDA/UDA Binding if member datatype is currency
            if (member.getDataType() == HspConstants.DATA_TYPE_CURRENCY && member.getId() != member.getDimId()) {
                if (uda == null) {
                    //add uda
                    uda = new HspUDA();
                    uda.setDimId(member.getDimId());
                    uda.setUdaValue(HspConstants.UDA_HSP_CURRENCY);
                    uda.setRemovable(0); //locking UDA so that user cannot add/remove manually
                    addUDA(uda, sessionId);
                }
                boolean addUdaBinding = true;
                HspUDABinding[] markedForSave = member.getUDAsToBeSaved();
                if (!HspUtils.isNullOrEmpty(markedForSave)) {
                    for (int i = 0; i < markedForSave.length; i++) {
                        HspUDABinding binding = markedForSave[i];
                        if (binding.getUDAId() == uda.getId()) { // Check if it is already added for save.
                            addUdaBinding = false;
                            break;
                        }
                    }
                } else {
                    if (currencyUDAExists)
                        addUdaBinding = false;
                }

                if (addUdaBinding) {
                    HspUDABinding udaBinding = new HspUDABinding();
                    udaBinding.setUDAId(uda.getId());
                    udaBinding.setMemberId(member.getId());

                    UDAsToBeSavedList.add(udaBinding);

                    HspUDABinding[] in = UDAsToBeSavedList.toArray(new HspUDABinding[UDAsToBeSavedList.size()]);
                    member.setUDAsToBeSaved(in);
                }

            } else { //delete UDA/UDA Binding if member datatype is non currency and uda/uda binding exists
                if (currencyUDAExists) {
                    for (Iterator<HspUDABinding> it = UDAsToBeSavedList.iterator(); it.hasNext(); ) {
                        HspUDABinding binding = it.next();
                        if (binding.getUDAId() == uda.getId()) {
                            it.remove();
                        }
                    }
                    HspUDABinding[] in = UDAsToBeSavedList.toArray(new HspUDABinding[UDAsToBeSavedList.size()]);
                    member.setUDAsToBeSaved(in);

                    //delete UDA
                    if (uda != null) {
                        Vector<HspUDABinding> bindingsForUda = getUDABindingsForUDA(uda.getId());
                        int size = bindingsForUda.size();
                        for (HspUDABinding binding : bindingsForUda) {
                            if (binding.getMemberId() == member.getId()) {
                                size--;
                            }
                        }
                        if (size == 0) {
                            if (uda.isObjectLocked()) {
                                uda.setRemovable(1);
                            }
                            deleteUDA(uda, sessionId);
                        }
                    }
                }
            }
        }
    }

    /**
     * Gets the count of members as would be sent over to Essbase, inclusive of root member,
     * in specified dimension as applicable for specified plan type.
     *
     * @param dimension the dimension whose member count is sought.
     *
     * @param planType only members belonging to this plan type are included in count.
     *
     * @param   sessionId identifies the caller for security purposes.
     *
     * @return the count of members as created on Essbase after a successful cube refresh.
     */
    public int getNumDimMembers(HspDimension dimension, int planType, int sessionId) {
        Vector<HspMember> members = getDimMembers(dimension.getId(), false, sessionId);
        int mbrCount = CollectionUtils.countMatches(members, HspPredicateUtils.usedInPredicate(planType));
        return mbrCount + 1; // Add 1 to size because root member was discounted.
    }

    /**
     * Gets the least sparse or densest sparse dimension from within specified cube.
     * <p>
     * The least sparse dimension is the dimension with most members in it. That is,
     * this method iterates through all sparse dimensions, from within this cube,
     * gets member count per each sparse dimension, and returns the sparse dimension
     * with highest member count.
     *
     * @param planType the plan type of cube whose least sparse dimension is sought.
     *
     * @param sparseDimIDsToExclude the set of sparse dimensions which should be exlcuded from least sparse
     * consideration. If this happens, this method returns next least sparse dimension. If
     * the sparseDimIDsToExclude set is empty, then, this method will not apply any exclusions. If
     * the sparseDimIDsToExclude set memebrs are not found from within dimensions belonging to this
     * plantype, then, this method will not apply any exclusions and also will not throw exceptions.
     *
     * @param sessionId identifies the caller for security purposes.
     *
     * @return the least sparse dimension from among dimensions belonging to this cube
     * identified  by its plan type.
     */
    public HspDimension getLeastSparseDimension(int planType, Set<Integer> sparseDimIDsToExclude, int sessionId) {
        Vector<HspDimension> baseDims = getBaseDimensions(planType, sessionId);
        Collection<HspDimension> sparseDims = CollectionUtils.select(baseDims, HspPredicateUtils.dimensionDensityPredicate(HspConstants.kDataDensitySparse, planType));
        excludeDimensionIDsFromCollection(sparseDims, sparseDimIDsToExclude);
        HspDimension dimWithHighestMemberCount = null;
        int highestMemberCount = 0;
        Iterator<HspDimension> itr = sparseDims.iterator();
        while (itr.hasNext()) {
            HspDimension sparseDim = itr.next();
            if (dimWithHighestMemberCount == null) {
                dimWithHighestMemberCount = sparseDim;
                highestMemberCount = getNumDimMembers(sparseDim, planType, sessionId);
            } else {
                int memberCount = getNumDimMembers(sparseDim, planType, sessionId);
                if (memberCount > highestMemberCount) {
                    dimWithHighestMemberCount = sparseDim;
                    highestMemberCount = memberCount;
                }
            }
        }
        return dimWithHighestMemberCount;
    }

    /**
     * This method excludes dimension IDs, specified by dimIDsToExclude, from specified dims collection.
     * If dimIDsToExclude set is empty, then, nothing is excluded. If specified dimIDsToExclude is not found
     * in specified dims collection, then, nothing is excluded and also no exception is thrown.
     * This method modified specified dims collection itself should a matching dimIDsToExclude be found.
     * @param dims the collection from which to exclude.
     * @param dimIDsToExclude the set of dimension IDs to exclude.
     */
    private void excludeDimensionIDsFromCollection(Collection<HspDimension> dims, Set<Integer> dimIDsToExclude) {
        // Don't exclusion needed if set dimIDsToExclude is empty.
        if (HspUtils.isNullOrEmpty(dimIDsToExclude))
            return;

        Iterator<HspDimension> itr = dims.iterator();
        while (itr.hasNext()) {
            HspDimension dim = itr.next();
            if (dimIDsToExclude.contains(dim.getId())) {
                itr.remove();
            }
        }
    }

    private void resetBufferResourceDetails() {
        String sInstanceName = System.getProperty("weblogic.Name", "unknown");
        if (sInstanceName.equals("EPMServer0")) {
            if (hspSQL == null) {
                throw new HspRuntimeException("MSG_HSPSQL_NULL");
            }
            Connection conn = null;
            try {
                conn = hspSQL.getConnection();
                if (conn == null) {
                    throw new HspRuntimeException("MSG_RDB_CONN_FAIL");
                }
                hspSQL.executeUpdate("SQL_RESET_BUF_RESOURCE_AND_OPRTS", conn);
                conn.commit();
            } catch (SQLException e) {
                Properties props = new Properties();
                props.setProperty("ERROR_MSG", e.getMessage());
                throw new HspRuntimeException("MSG_MIG_FAILED_RELEASE_LOCK", props, e);
            } finally {
                HspUpgradeHelper.releaseConnection(hspSQL, conn);
            }
        }
    }

    private void logBufferResourceDetails() {
        Connection connection = null;
        PreparedStatement pstmt = null;
        try {
            connection = hspSQL.getConnection();
            pstmt = connection.prepareStatement("SELECT BUF_RESOURCE_RESERVE, BUF_RESOURCE_USAGE, OPERATIONS, RESOURCE_USED, CUBE_NAME FROM HSP_ESSBASE_CUBE");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                logger.info(HspODLMsgs.INF_MRA_INFORMATION, "Buffer resource reserve: " + rs.getInt(1) + " Buffer resource usage: " + rs.getInt(2) + " Operations: " + rs.getInt(3) + " Resource used: " + rs.getInt(4) + " for Cube: " + rs.getString(5));
            }
        } catch (Exception e) {
            logger.info(HspODLMsgs.INF_MRA_INFORMATION, "Error while retrieving buffer resource details : " + e.getMessage());
        } finally {
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (Exception e) {
                    logger.info(HspODLMsgs.INF_MRA_INFORMATION, "Error while closing PreparedStatement: " + e.getMessage());
                }
            }
            if (connection != null)
                hspSQL.releaseConnection(connection);
        }
    }

    /**
     * Adds entry for the customization if the passed member  is seeded & modified outside of the enable features.
     * @param member
     * @param sessionId
     * @throws Exception
     */
    private void trackMemberChangesIfSeeded(HspMember member, int sessionId) throws Exception {
        //If seeded member & the user is not module user, track the changes
        boolean isObjectSeeded = hspJS.getFeatureDB(sessionId).isObjectSeeded(member.getId(), member.getObjectType());
        boolean shouldTrack = HspModuleInfo.getModuleUser() == null || (!HspModuleInfo.getModuleUser().equals(HspModuleInfo.MODULE_USER));
        List<HspDiff> diffList = null;
        if (isObjectSeeded && shouldTrack) {
            ReadOnlyCachedObject objInCache = hspJS.getFeatureDB(sessionId).getPlanningObject(member.getObjectName(), member.getObjectType(), sessionId, member.getParentId());
            diffList = member.getDiffList(hspJS, objInCache, sessionId);

            //Below condition is satisfied for Shared Members for which UsedIn has been changed i.e. diffList will NOT be EMPTY
            if (member.isSharedMember() && diffList != null && !diffList.isEmpty()) {
                HspDiffUtil.addParameterToDiffList("PARENT_NAME", member.getParent().getName(), diffList);
            }

            if (diffList != null && diffList.size() > 0) {
                int artifactId = hspJS.getFeatureDB(sessionId).getModuleArtifactDetailByObjId(member.getId(), member.getObjectType()).getId();
                hspJS.getFeatureDB(sessionId).addAuditRecord(HspActionSet.UPDATE, diffList, artifactId, sessionId);
            }
        }

    }

    /**
     * Returns memnor for the specified member id.
     *
     * @param   dimId           Id of the Dimension conatining the member
     * @param   mbrId           Id of the member to be retrieved
     *
     * @return  member memnor
     */
    public int getMemnor(int dimId, int mbrId) {
        Map<Integer, Integer> memnorByMemberIdMap = getMemnorMap(dimId);
        Integer memnor = memnorByMemberIdMap.get(mbrId);
        return memnor == null ? -1 : memnor;

    }

    /**
     * Returns a map for the specified dimension id.
     *
     * @param   dimId           Id of the Dimension conatining the member
     *
     * @return  a memnor map for a dimension
     */
    public Map<Integer, Integer> getMemnorMap(int dimId) {
        // TODO: In the DEDB version, get this form the cache directly to avoid ptprops
        HspMember rootMember = getDimMember(dimId, dimId);
        HspUtils.verifyArgumentNotNull(rootMember, "rootMember");
        synchronized (rootMember) {
            Map<Integer, Integer> memnorByMemberIdMap = memnorTimedMap.get(dimId);
            if (memnorByMemberIdMap == null) {
                memnorByMemberIdMap = new HashMap<>();
                int memnor = 1;
                for (HspMember member : (List<HspMember>)rootMember.getDescendants(true, false)) {
                    memnorByMemberIdMap.put(member.getId(), memnor++);
                }
                memnorTimedMap.put(dimId, Collections.unmodifiableMap(memnorByMemberIdMap));
            }
            return memnorByMemberIdMap;
        }
    }

    /**
     * Remove a map for the specified dimension id.
     *
     * @param   dimId           Id of the Dimension
     *
     */
    public void removeMemnorMap(int dimId) {
        memnorTimedMap.remove(dimId);
    }

    /**
     * Whether the memnorTimedMap is empty.
     *
     * @return true if memnorTimedMap is empty, otherwise false
     * every 60 mins, memnorTimedMap will be cleaned up or cube refresh event happened, memnorTimedMap will be cleaned up
     *
     */
    public boolean isMemnorTimedMapEmpty() {
        return memnorTimedMap.isEmpty();
    }
}
