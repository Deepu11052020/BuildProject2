import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.{col, current_timestamp, format_number, initcap, to_date, when}

import java.util.Properties

object Main {
  def main(args: Array[String]): Unit = {
    // Define Spark configuration
    val sparkConf = new SparkConf()
      .setAppName("superMarket Project")
      .setMaster("local[1]")
    // Create SparkSession
    val spark = SparkSession.builder()
      .config(sparkConf)
      .enableHiveSupport()
      .getOrCreate()

    // Define schema for the supermarket sales data
    val superSchema = "InvoiceID String, BranchId Int, Customer_type String, Gender String,Product_line String, Unit_price Float, Quantity Integer, Tax_5_percent Float,  Total Float, Date1 String, Time1 String, Payment String, cogs Float,gross_margin_per Float, gross_income Float, Rating Float"
    val superdf = spark.read
      .option("header", true)
      .schema(superSchema)
      .csv(args(0))
    // .csv("C:\\Users\\deepu\\Documents\\Project_SuperMarket\\supermarket_sales.csv")
    val sortedSuperdf = superdf.orderBy("InvoiceID")
    val superdf_D = sortedSuperdf.dropDuplicates()
    val superdfFilled = superdf_D.na.fill("Unknown")
    val convertedDF = superdfFilled.withColumn("Gender", when(col("Gender") === "F", "Female").otherwise("Male"))
    val decimaldf = convertedDF.withColumn("Tax_5_percent", format_number(col("Tax_5_percent").cast("Decimal(10,2)"), 2))
      .withColumn("Total", format_number(col("Total").cast("Decimal(10,2)"), 2))
    val superMarketdf_cleaned = decimaldf.withColumn("Created_date",current_timestamp())
/*
    // Load the second CSV file
    val df2 = spark.read.option("header", true).csv(args(6))
    // Append the second dataframe to the first one
    val mergedDF = superMarketdf_cleaned.union(df2)
    // Write the merged dataframe to the output path
   // mergedDF.write.option("header", true).csv(args(1))
*/
    //define schema for Branch
    val branchSchema = "BranchId Int, Branch_Name String, City_Name String, Start_Date String, End_Date String"
    val branchdf= spark.read
      .option("header", true)
      .schema(branchSchema)
      .csv(args(2))
    // .csv("C:\\Users\\deepu\\Documents\\Project_SuperMarket\\BranchCity.csv")
    val citydf = branchdf.withColumn("City_Name", initcap(col("City_Name")))
    val branchdf_cleaned = citydf.withColumn("Created_date",current_timestamp())
    //define schema for product Table
    val productLine = "ProductLine_Id Int,ProductLine_Desc String,Start_Date String,End_Date String"
    var producthdf= spark.read
      .option("header", true)
      .schema(productLine)
      .csv(args(4))
    //.csv("C:\\Users\\deepu\\Documents\\Project_SuperMarket\\ProductLine.csv")
    val formattedproducthdf= producthdf.withColumn("Start_Date", to_date(col("Start_Date"), "dd/MM/yyyy"))
      .withColumn("End_Date", to_date(col("End_Date"), "dd/MM/yyyy"))
    val productdf_desc = formattedproducthdf.withColumn("ProductLine_Desc", initcap(col("ProductLine_Desc")))
    val productdf_cleaned = productdf_desc.withColumn("Created_date",current_timestamp())
    superMarketdf_cleaned.show(100, false)
    branchdf_cleaned.show()
    productdf_cleaned.show()

    superMarketdf_cleaned.coalesce(1).write.option("header", true).mode("overwrite").csv(args(1))
    branchdf_cleaned.coalesce(1).write.option("header", true).mode("overwrite").csv(args(3))
    productdf_cleaned.coalesce(1).write.option("header", true).mode("overwrite").csv(args(5))


    //Hive
    superMarketdf_cleaned.write.mode("overwrite").saveAsTable("ukusmar.accounts_table")
    branchdf_cleaned.write.mode("overwrite").saveAsTable("ukusmar.customers_table")
    productdf_cleaned.write.mode("overwrite").saveAsTable("ukusmar.transactions_table")




  }
}

/*

import spark.implicits._
  //My sQL
  // Define JDBC connection properties
  val url = "jdbc:mysql://localhost:3306/testdb"
  val username = "root"
  val password = "Kittians@01"
  val connectionProperties = new Properties()
  connectionProperties.put("user", username)
  connectionProperties.put("password", password)
  val superMarket = "superMarket"
  //val branch ="branch"
  //val ProductLine ="ProductLine"

  // Write DataFrame to MySQL
  decimalDF.write.jdbc(url, superMarket, connectionProperties)
  //branchdf_cleaned.write.jdbc(url,branch,connectionProperties)
  //product_cleaned.write.jdbc(url, ProductLine, connectionProperties)
  //Appending
  //cleanedDF.write.mode("Append").jdbc(url,superMarket,connectionProperties)
 */
