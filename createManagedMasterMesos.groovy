/*** BEGIN META {
 "name" : "Create a Managed Master in CloudBees Jenkins Enterprise",
 "comment" : "This script creates a Mesos Managed Master programmatically similarly to what can be done through the UI. 
 It has been tested with version 1.11.22 of CloudBees Jenkins Enterprise",
 "parameters" : [],
 "core": "2.249.2.4",
 "authors" : [
 { name : "Allan Burdajewicz" }
 ]
 } END META**/

import com.cloudbees.jce.masterprovisioning.mesos.HealthCheckConfiguration
import com.cloudbees.jce.masterprovisioning.mesos.MesosMasterProvisioning
import com.cloudbees.opscenter.server.model.ManagedMaster
import com.cloudbees.opscenter.server.properties.ConnectedMasterLicenseServerProperty
import com.cloudbees.opscenter.server.properties.ConnectedMasterOwnerProperty

/*****************
 * INPUTS        *
 *****************/

/**
 * The master name is mandatory
 */
String  masterName = "mm-from-groovy"

/**
 * Following attributes may be specified. The values proposed are the default from version 2.2.9 of Master Provisioning
 *
 * Note: If not setting properties explicitly, the defaults will be used.
 */
/* Master */
String  masterDisplayName = ""
String  masterDescription = ""
String  masterPropertyOwners = ""
Integer masterPropertyOwnersDelay = 5

/* Master Provisioning */
Integer mesosDisk = 50
Integer mesosMemory = 3072
/**
 * Since version 2.235.4.1, we recommend not using the Heap Ratio. Instead add `-XX:MinRAMPercentage` and 
 * `-XX:MaxRAMPercentage` to the Java options. For example, a ratio of 0.5d translate to a percentage of 50: 
 * `-XX:MinRAMPercentage=50.0 -XX:MaxRAMPercentage=50.0`
 *
 * See https://support.cloudbees.com/hc/en-us/articles/204859670-Java-Heap-settings-best-practice and 
 * https://docs.cloudbees.com/docs/release-notes/latest/cloudbees-ci/modern-cloud-platforms/2.235.4.1.
 */
Double  mesosMemoryRatio = null
Double  mesosCpus = 1
Boolean mesosAllowExternalAgents = false
String  mesosClusterEndpointId = "default"
String  mesosEnvVars = ""
String  mesosJavaOptions = "-XX:MinRAMPercentage=50.0 -XX:MaxRAMPercentage=50.0"
String  mesosJenkinsOptions = ""
String  mesosSystemProperties = ""
String  mesosImage = 'CloudBees Jenkins Enterprise 2.249.2.4'
Integer mesosGracePeriodSeconds = 1200
Integer mesosIntervalSeconds = 60

/*****************
 * CREATE MASTER *
 *****************/

/**
 * Create a Managed Masters with just a name (this will automatically fill required values for id, idName, grantId, etc...)
 * Similar to creating an iten in the UI
 */
ManagedMaster newInstance = jenkins.model.Jenkins.instanceOrNull.createProject(ManagedMaster.class, masterName)
newInstance.setDescription(masterDescription)
newInstance.setDisplayName(masterDisplayName)

/********************
 * CONFIGURE MASTER *
 ********************/

/**
 * Configure the Master provisioning details. Refer to the `config.xml` for more details.
 * Similar to configuring a Managed Master from the UI
 */
MesosMasterProvisioning masterProvisioning = new MesosMasterProvisioning()
masterProvisioning.setDomain(masterName.toLowerCase())

/**
 * Apply Managed Master provisioning configuration (similar to what is configured through the Managed Master UI)
 * Note: If not setting properties explicitly, the defaults will be used.
 */
masterProvisioning.setDisk(mesosDisk)
masterProvisioning.setMemory(mesosMemory)
if(mesosMemoryRatio) {
    masterProvisioning.setHeapRatio(new com.cloudbees.jce.masterprovisioning.Ratio(mesosMemoryRatio))
    /**
     * For versions earlier than 2.235.4.1 (Master Provisioning plugin 2.5.6), use setRatio
     * masterProvisioning.setRatio(mesosMemoryRatio)
     */
}
masterProvisioning.setCpus(mesosCpus)
masterProvisioning.setAllowExternalAgents(mesosAllowExternalAgents)
masterProvisioning.setClusterEndpointId(mesosClusterEndpointId)
masterProvisioning.setEnvVars(mesosEnvVars)
masterProvisioning.setJavaOptions(mesosJavaOptions)
masterProvisioning.setJenkinsOptions(mesosJenkinsOptions)
masterProvisioning.setImage(mesosImage)
masterProvisioning.setSystemProperties(mesosSystemProperties)
HealthCheckConfiguration hc = new HealthCheckConfiguration()
hc.setGracePeriodSeconds(mesosGracePeriodSeconds)
hc.setIntervalSeconds(mesosIntervalSeconds)
masterProvisioning.setHealthCheckConfiguration(hc)

/**
 * Provide Master item general configuration (similar to what is configured through the Master UI)
 * Note: If not setting properties explicitly, the defaults will be used.
 */
if (masterPropertyOwners != null && !masterPropertyOwners.isEmpty()) {
    newInstance.getProperties().replace(new ConnectedMasterOwnerProperty(masterPropertyOwners, masterPropertyOwnersDelay))
}
newInstance.getProperties().replace(new ConnectedMasterLicenseServerProperty(new ConnectedMasterLicenseServerProperty.DescriptorImpl().defaultStrategy()))

/**
 * Save the configuration
 */
newInstance.setConfiguration(masterProvisioning)
newInstance.save()

/**
 * Retrieve the master from the API and print the details of the created Managed Master
 */
def instance = jenkins.model.Jenkins.instanceOrNull.getItemByFullName(newInstance.fullName, ManagedMaster.class)
println "${instance.name}"
println " id: ${instance.id}"
println " idName: ${instance.idName}"

/******************************
 * PROVISION AND START MASTER *
 ******************************/

instance.provisionAndStartAction()
println "Started the master..."
return