#!/bin/bash
cd /Users/ariven/1-jmiopenatom/openatom-system/src/main/java/edu/jmi/openatom/server/openatomsystem

cp dto/request/RequestCreateRecruitmentCampaignDTO.java dto/request/RequestCreateSiteFormDTO.java
cp dto/request/RequestUpdateRecruitmentCampaignDTO.java dto/request/RequestUpdateSiteFormDTO.java

sed -i '' 's/RecruitmentCampaign/SiteForm/g' dto/request/RequestCreateSiteFormDTO.java dto/request/RequestUpdateSiteFormDTO.java
sed -i '' 's/recruitmentCampaign/siteForm/g' dto/request/RequestCreateSiteFormDTO.java dto/request/RequestUpdateSiteFormDTO.java
sed -i '' 's/Recruitment Campaign/Site Form/g' dto/request/RequestCreateSiteFormDTO.java dto/request/RequestUpdateSiteFormDTO.java
sed -i '' 's/campaign/form/g' dto/request/RequestCreateSiteFormDTO.java dto/request/RequestUpdateSiteFormDTO.java
sed -i '' 's/Campaign/SiteForm/g' dto/request/RequestCreateSiteFormDTO.java dto/request/RequestUpdateSiteFormDTO.java
