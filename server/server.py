# -*- coding: utf-8 -*-

'''

@author: Chung
'''

import threading
import cherrypy
import os
import time
import pickle
import simplejson
import json
import datetime
import shutil
import xlsxwriter
import zipfile
import sqlite3
import traceback
from PIL import Image
import signal
import sys
from libs.logger import get_logger



DataPath = os.path.join(os.getcwd(), "data")
# Below here there are the folders with the product number
Product_DataPath = os.path.join(DataPath, "products")
Working_DataPath = os.path.join(DataPath, "working")
PrintFiles_DataPath = os.path.join(DataPath, "files")

Main_DB = os.path.join(DataPath, "data.db")
log = get_logger('Backend API')

# This is the API that the app will call
class WebAPI(object):
    
    def __init__(self, iface):
        self.control = iface
        self.ip = ""
        self.port = ""
        print ("WebAPI starting")


    # Function called by the android to fetch the Product Data and the Category data
    @cherrypy.expose
    def fetchCategoryData(self, catid):
        print ("Fetching Category Data ", catid)
        conn = sqlite3.connect(Main_DB)
        list_prodids = self.control.db_getProductFromCategory(conn, int(catid))
        resp = self.control.getUnformatedProducts(list_prodids)
        conn.close()
        return simplejson.dumps(resp)

        # This shouldn't error here
        # return simplejson.dumps(self.control.getAllUnformattedProducts())


    # Function that gets the list of all categories and the products to show on the tablet
    @cherrypy.expose
    def fetchListing(self):
        conn = sqlite3.connect(Main_DB)
        catpairs = self.control.db_getAllCategories(conn)
        blacklist = self.control.db_getAllCategoryProducts(conn)
        conn.close()
        products = self.control.getAllUnformattedProducts(blacklist)
        listing = catpairs + products
        # Remove the ids from the all products
        return simplejson.dumps(listing)

    # This function will get a zipfile from the android and parse the data
    # Adds and updates the data
    @cherrypy.expose
    def addUpdateProcedure(self, id, file):
        # Check if id exists
        if self.control.checkIDExists(int(id)):
            try:
                zip_title = file.filename
                # This is the open file object
                obj = file.file
                # This reads the data
                data = obj.read()
                # Updates the files
                unzipped_list = self.control.unpackageZipFile(zip_title, data)
                # Moving files to the folder
                folder_path = os.path.join(Product_DataPath, id)
                if (os.path.isdir(folder_path) and unzipped_list != None):
                    for f in unzipped_list:
                        path = os.path.join(Working_DataPath, f)
                        dest_path = os.path.join(folder_path, f)
                        shutil.copy(path, dest_path)
                # Update the dicitonary
                # Delete the photos not used
                if self.control.updateProduct(id):
                    # Delete the working files
                    self.control.deleteAllFiles(Working_DataPath)
                    # Success
                    return
                else:
                    # Product data not correct
                    return self.httpErrorMessage(400, u'Product Data not Correct / 商品データのエラーが発生しました。')
            # If Zipfile is wrong
            except Exception as e:
                return self.httpErrorMessage(400, u"Zip file not accepted / Zipファイルのフォーマットは正しいではありません。")
        else:
            return self.httpErrorMessage(400, u"ID doesn't exists / ID存在しないエラー。")


    # This function will reorder the position of the procedure
    @cherrypy.expose
    def reorderProcedure(self, metadata):
        parsed = json.loads(metadata)
        # Check if it exits on the lis
        if self.control.checkIDExists(int(parsed["id"])):
            if self.control.updateMetadata(parsed) != None :
                # Success
                return
            else:
                return self.httpErrorMessage(400, u'Product Data not Correct / 商品データのエラーが発生しました。')

        return self.httpErrorMessage(400, u"ID doesn't exists / ID存在しないエラー。")

    # This function will delete the procedure from the system
    @cherrypy.expose
    def deleteProcedure(self, metadata):
        parsed = json.loads(metadata)
        if self.control.checkIDExists(int(parsed["id"])):
            # Overwrite the metadata file
            productpath = self.control.updateMetadata(parsed)
            if productpath != None:
                # Delete
                self.control.deleteProduct(parsed, productpath)
                # Success
                return
            else:
                return self.httpErrorMessage(400, u'Product Data not Correct / 商品データのエラーが発生しました。')

        return self.httpErrorMessage(400, u"ID doesn't exists / ID存在しないエラー。")


    # For WEB USE Now -> Add new Category to the db
    # DO NOT Print the categoryname as it will have a encoding error
    @cherrypy.expose
    def addNewCategory(self, categoryname):
        """
            API needs   name: new_category_name,
                          
        """
        parsed = simplejson.loads(categoryname)
        conn = sqlite3.connect(Main_DB)
        result = self.control.db_addNewCategories(conn, parsed["name"])
        if result:
            catg = self.control.db_getCategory(conn, parsed["name"])
            catg.insert(0, parsed["name"])
            conn.close()
            return simplejson.dumps(catg)
        else:
            conn.close()
            error = "Error"
            raise cherrypy.HTTPError(400, error)


    # Function to get Categories list for the autocomplete on the WEB
    @cherrypy.expose
    def getAllCategories(self):
        conn = sqlite3.connect(Main_DB)
        resp = []
        categories = self.control.db_getAllCategories(conn)
        for val in categories:
            resp.append(val[0])
        conn.close()
        return simplejson.dumps(resp)


    # Get All Categories for WEB Category Table
    @cherrypy.expose
    def getCategoryTable(self):
        conn = sqlite3.connect(Main_DB)
        categories = self.control.db_getAllCategories(conn)
        conn.close()
        return simplejson.dumps(categories)


    # Edit the Category
    @cherrypy.expose
    def editCategory(self, category):
        """
            API needs   name: new_category_name,
                        catid: category_id
                                      
        """
        parsed = simplejson.loads(category)
    #     TODO Check that the data is correct
        conn = sqlite3.connect(Main_DB)
        resp = self.control.db_updateCategory(conn, parsed)
        if resp != None:
            self.control.updateLocalCategories(conn)
            conn.close()
            return simplejson.dumps(resp)
        else:
            error = "Category could not be found, please check the admins"
            raise cherrypy.HTTPError(400, error)


    # Remove all the elements under this category
    @cherrypy.expose
    def removeCategory(self, catid):
        """
            API needs  categoryid 
        """
        conn = sqlite3.connect(Main_DB)
        # Remove all the connections on the product
        products = self.control.db_getProductFromCategory(conn, catid)
        for id in products:
            update = self.control.db_updateProduct(conn, id)
            # If there was an error in the update
            if not update:
                conn.close()
                return None
        self.control.updateLocalCategories(conn)
        catgid = self.control.db_removeCategory(conn, catid)
        conn.close()
        return catgid

    # @cherrypy.expose
    # def getNumberFiles(self, productid):
    #     return self.control.getNumberFiles(productid)

    # This function will get the procedures from the system
    # Sends the data to the device
    @cherrypy.expose
    def getProductData(self, productid):
        # Checking if ID exists
        if self.control.checkIDExists(productid):
            # Get the file number
            filecount = self.control.getNumberFiles(productid)
            try:
                zipfile = self.control.makeZipFile(productid)
                cherrypy.response.headers["Content-Type"] = "application/zip"
                cherrypy.response.headers["Content-Disposition"] = 'attachment; filename="{files}.zip"'.format(files=str(filecount))
                #     Send the zipfile
                if zipfile != None:
                    with open(zipfile, 'rb') as fp:
                        return fp.read()
                else:
                    return None
            except Exception as e:
                return self.httpErrorMessage(500 ,u"Problem with zipping file / Zipファイルエラーが発生しました。")
        else:
            # This should not run
            error = u"ID doesn't exists / ID存在しないエラー。"
            return self.httpErrorMessage(400, error)



    # Edit the Product Name for the Android
    @cherrypy.expose
    def editProductName(self, product):
        parsed = simplejson.loads(product)
        conn = sqlite3.connect(Main_DB)
    #     Check product
        if self.control.checkProductDataFromAndroid(parsed):
            newdata = self.control.parseAndroidKeys(parsed)
            resp = self.control.addProductData(newdata, conn)
            conn.close()
            return simplejson.dumps(resp)
        else:
            error = u"Product Data not correct <br />" \
                    u" 商品データのエラーが発生しました。"
            raise cherrypy.HTTPError(400, error)


    # NOTE - WEB USE
    # This function will update the product from the resources
    @cherrypy.expose
    def editProduct(self, product):
        """
            API needs   number: id_number,
                        name: product_name,
                        date: created date,
                        last_date: updated_date,
                        creator: name_creator,
                        category: category_id (Created from category)
        """
        parsed = simplejson.loads(product)
        conn = sqlite3.connect(Main_DB)
        # Check if product is correct
        if self.control.checkProductDataFromWeb(parsed):
            resp = self.control.addProductData(parsed, conn)
            conn.close()
            return simplejson.dumps(resp)
        else:
            conn.close()
            error = u"Product Data not correct <br />" \
                    u" 商品データのエラーが発生しました。"
            raise cherrypy.HTTPError(400, error)

    # This function will return all the products that were added to they system
    # NOTE - WEB USE
    @cherrypy.expose
    def getAllProducts(self):
        # This shouldn't error here
        return simplejson.dumps(self.control.getAllProducts())

    # NOTE - WEB USE
    # This function will create the resources for saving the product
    @cherrypy.expose
    def addProduct(self, product):
        """
            API needs   number: id_number,
                        name: product_name,
                        date: created date,
                        last_date: updated_date,
                        creator: name_creator,
                        category: category_id (Created from category)
        """
        parsed = simplejson.loads(product)
        conn = sqlite3.connect(Main_DB)
        # Check if product is correct
        if self.control.checkProductDataFromWeb(parsed):
            resp = self.control.addProductData(parsed, conn)
            conn.close()
            return  simplejson.dumps(resp)
        else:
            conn.close()
            error = u"Product Data not correct <br />" \
                    u" 商品データのエラーが発生しました。"
            raise cherrypy.HTTPError(400, error)


    # NOTE - WEB USE
    # This function will delete the product from the resources
    @cherrypy.expose
    def deleteProduct(self, productid):
        """
            API needs  productid = id_number of product
        """
        conn = sqlite3.connect(Main_DB)
        db_resp = self.control.db_deleteProduct(conn, productid)
        # Error check on db deletion
        if db_resp == None:
            conn.close()
            error = u"Product Data not correct <br />" \
                    u" 商品データのエラーが発生しました。"
            raise cherrypy.HTTPError(400, error)

        resp = self.control.deleteProductData(productid)
        conn.close()
        if resp != None:
            return simplejson.dumps(resp)
        else:
            error = u"Product Data not correct <br />" \
                    u" 商品データのエラーが発生しました。"
            raise cherrypy.HTTPError(400, error)


    # NOTE - WEB USE
    # Function called to create the excel file
    @cherrypy.expose
    def makePrintableFile(self, productid):
        self.control.deleteAllFiles(PrintFiles_DataPath)
        if self.control.checkIDExists(productid):
            try:
                path = self.control.createExcelFile(productid)
                cherrypy.response.headers["Content-Type"] = "application/download"
                cherrypy.response.headers["Content-Disposition"] = 'attachment; filename="%s.xlsx"'%(productid)
                with open(path, 'rb') as fp:
                    return fp.read()
            except Exception as e:
                error = u"Error Excel file couldn't be created <br />" \
                       u"エラー　ファイルの作成は失敗しました。"
                raise cherrypy.HTTPError(500, error)
        else:
            error = u"Error Product ID not found, please Refresh.  <br />" \
                   u"エラー　管理番号は見つからない、プラグインのページをリロードしてください。"
            raise cherrypy.HTTPError(400, error)


    # Function to check the device
    @cherrypy.expose
    def checkDevice(self):
        resp = ["True"]
        return json.dumps(resp)


#     Function to set the error for Android to pick up
    def httpErrorMessage(self, code, msg=None):
        cherrypy.response.status = code
        return msg


class ClientThread(threading.Thread):
    
    def __init__(self):
        threading.Thread.__init__(self)
        log.info("STARTING Client")
        self.alive = 0

        # This function creates all the necessary folders for the application to start
        if not os.path.exists(Product_DataPath):
            os.makedirs(Product_DataPath)
        if not os.path.exists(Working_DataPath):
            os.makedirs(Working_DataPath)
        if not os.path.exists(PrintFiles_DataPath):
            os.makedirs(PrintFiles_DataPath)

        # Check if database exists if not Create a database
        self.createloadDB()

        # Saves the metadata into a dictionary
        self.data = {}
        self.products = {}
        self.categories = {}
        # Needs to be json
        self.metadatafile = "metadata.json"

        self.InitializePlugin()
        self.start()

    ##########################################################################
    ######
    ######      DB Functions
    ######
    ##########################################################################

    def stopthread(self):
        self.alive = 1

    # Create the DB
    def createloadDB(self):
        if not os.path.isfile(Main_DB):
            conn = sqlite3.connect(Main_DB)
            c= conn.cursor()
            # Create Main Table
            c.execute("""
                     CREATE TABLE products
                     (
                     id INTEGER PRIMARY KEY AUTOINCREMENT,
                     catg_name INTEGER,
                     options TEXT,
                     FOREIGN KEY (catg_name) REFERENCES categories(id)
                     );
                     """)
            # Create Categories table
            c.execute("""
                     CREATE TABLE categories
                     (
                     id INTEGER PRIMARY KEY AUTOINCREMENT,
                     name TEXT NOT NULL
                     );
                     """)
            conn.commit()
            conn.close()


    # Get all the category ids from the products
    def db_getAllCategoryProducts(self, conn):
        cursor = conn.cursor()
        returndata = []
        val = cursor.execute("SELECT id, catg_name FROM products").fetchall()
        if val != None:
            for i in val:
                if i[1] != None:
                    returndata.append(i[0])
        return returndata

    # ADD New Category
    def db_addNewCategories(self, conn, value):
        cursor = conn.cursor()
        try:
            cursor.execute('INSERT INTO categories(name) VALUES (?)', (value,))
            conn.commit()
            return True
        except sqlite3.OperationalError:
            return False

    # UPDATES The name of the Category
    def db_updateCategory(self, conn, value):
        cursor = conn.cursor()
        try:
            cursor.execute("UPDATE categories SET name = ? WHERE id = ?", (value["name"], value["catid"]))
            conn.commit()
        except sqlite3.OperationalError as e:
            return None
        return [value["name"], value["catid"]]


    # DEL category but needs to update all the products
    def db_removeCategory(self, conn, catid):
        cursor = conn.cursor()
        try:
            cursor.execute("DELETE FROM categories WHERE id = ?", (catid,))
            conn.commit()
        except sqlite3.OperationalError:
            return None
        return catid

    # GET the id for the category name
    def db_getCategory(self, conn, catg_name):
        cursor = conn.cursor()
        returndata = []
        val = cursor.execute("SELECT id FROM categories WHERE name = ?",(catg_name.strip(),))
        if val != None:
            for i in val:
                returndata.append(i[0])
        return returndata

    # GET The list of all the Categories
    def db_getAllCategories(self, conn):
        cursor = conn.cursor()
        returndata = []
        val = cursor.execute("SELECT name, id FROM categories").fetchall()
        if val != None:
            for i in val:
                catdata = [i[0].strip(), i[1]]
                returndata.append(catdata)
        return returndata

    # GET All the categories for the Product
    def db_findCategoriesofProduct(self, conn):
        cursor = conn.cursor()
        returndata = {}
        val = cursor.execute("SELECT id, catg_name FROM products").fetchall()
        if val != None:
            for i in val:
                name = None
                if i[1] != None:
                    name = cursor.execute("SELECT name FROM categories WHERE id = ?",(i[1],)).fetchone()
                returndata[i[0]] = name
        return returndata

    # Function that adds the values that exists which are not in the DB
    # Just for error checking
    def db_addExistingValues(self):
        conn = sqlite3.connect(Main_DB)
        c = conn.cursor()
        # Check if not in the table
        for key, val in self.products.items():
            c.execute("SELECT id FROM products WHERE id = ?",(key,))
            data = c.fetchone()
            if data is None:
                self.db_addNewProduct(c, key)
                conn.commit()
        conn.close()

    # GET Product ID From the Category Name
    def db_getProductFromCategory(self,conn, catid):
        cursor = conn.cursor()
        returndata = []
        val = cursor.execute("SELECT id FROM products WHERE catg_name = ?",(catid,))
        if val != None:
            for i in val:
                returndata.append(i[0])
        return returndata

    # Add Product at init -> If products already exist
    # Just error checking
    def db_addNewProduct(self, cur, key, category=None):
        if category != None:
            try:
                cur.execute('INSERT INTO products(id, catg_name) VALUES (?)', (key,category))
            except sqlite3.OperationalError:
                return False
        else:
            try:
                cur.execute('INSERT INTO products(id) VALUES (?)', (key,))
            except sqlite3.OperationalError:
                return False

    # Delete Product from the DB
    def db_deleteProduct(self, conn, key):
        cursor = conn.cursor()
        try:
            cursor.execute('DELETE FROM products WHERE id = ?', (key,))
            conn.commit()
            return key
        except sqlite3.OperationalError:
            return None

    # Update Product DB
    def db_updateProduct(self, conn, key, category=None):
        # Get id for the name
        if category != None:
            category = self.db_getCategory(conn, category)[0]
        c = conn.cursor()
        try:
            c.execute('INSERT OR REPLACE INTO products(id,catg_name) VALUES (?,?);', (key, category))
            conn.commit()
            return True
        except sqlite3.OperationalError:
            return False



###################################################################################
    # Functions to create and deal with the data saved
####################################################################################

    # Function that will read files to populate data variables
    def InitializePlugin(self):
        filelist = os.listdir(Product_DataPath)
        # Add the products
        for file in filelist:
            product = self.getProductMetadata(file)
            if product != None:
                self.products[product["id"]] = product
        # Add categories
        conn = sqlite3.connect(Main_DB)
        self.categories = self.db_findCategoriesofProduct(conn)
        self.db_addExistingValues()

    # Get all products formatted for table + categories
    def getAllProducts(self):
        productslist = []
        for key, val in self.products.items():
            # No need procedure list since it's for the html
            element = [val["id"], val["name"], self.makeStringTime(val["created"]), self.makeStringTime(val["modified"]),val["author"], self.categories[int(val["id"])]]
            productslist.append(element)
        return productslist


    # Get all products for the Android
    def getAllUnformattedProducts(self, blacklist):
        productslist = []
        for key, val in self.products.items():
            if not val["id"] in blacklist:
                # Adding the procedure list since it's for the Android
                element = [val["id"], val["name"], val["created"],val["modified"], val["author"], val["procedure"]]
                productslist.append(element)
        return productslist


    # For Android use -> Gets all the products for a specific category
    def getUnformatedProducts(self, list):
        productslist = []
        for key, val in self.products.items():
            if key in list:
                # Adding the procedure list since it's for the Android
                element = [val["id"], val["name"], val["created"], val["modified"], val["author"], val["procedure"]]
                productslist.append(element)
        return productslist


    # Reads the product metadata
    def getProductMetadata(self, product):
        productfolder = os.path.join(Product_DataPath, product, self.metadatafile)
        if os.path.exists(productfolder):
            with open(productfolder, 'rb') as fp:
                productdata = json.loads(fp.read())
            if self.checkProductDataCorrectness(productdata):
                return productdata
        # Delete the product when the folder does not contain the metadata
        self.deleteProductData(product)
        return None


    # Update all categories from db
    def updateLocalCategories(self, conn):
        self.categories = self.db_findCategoriesofProduct(conn)


    # Function that checks that the ID exists
    def checkIDExists(self, productid):
        if int(productid) in self.products:
            return True
        return False


    # Checks that the product data is correct from the Android device
    # If error delete the folder ? , do not read the data
    def checkProductDataCorrectness(self, proctdata):
        # Check the product data keys and then values
        productdatakeys = ["name", "created", "author", "modified", "id", "procedure"]
        for key in productdatakeys:
            # Check key error
            if key not in proctdata:
                return False
            else:
                if key == "created" or key == "modified":
                    try:
                        self.makeStringTime(proctdata[key])
                    except Exception as e:
                        return False
                elif key == "id":
                    try:
                        int(proctdata[key])
                    except Exception as e:
                        return False
        return True


    # Checks the data that comes from the website
    def checkProductDataFromWeb(self, data):
        webkeys = ['number', 'name', 'creator', 'date', 'last_date']
        for key in webkeys:
            # Check key error
            if key not in data:
                return False
            else:
                if key == "date" or key == "last_date":
                    try:
                        self.makeStringTime(data[key])
                    except Exception as e:
                        return False
                elif key == "number":
                    try:
                        int(data[key])
                    except Exception as e:
                        return False
        return True


    # Checks the data that comes from Android
    def checkProductDataFromAndroid(self, data):
        androidkeys = ['created', 'id', 'modified', 'name', 'author']
        for key in androidkeys:
            if key not in data:
                return False
        return True

    # For ANDROID use -> Change the keys
    def parseAndroidKeys(self, data):
        data["number"] = str(data.pop("id"))
        data["date"] = data.pop("created")
        data["last_date"] = data.pop("modified")
        data["name"] = data.pop("name")
        data["creator"] = data.pop("author")
        return data


    # Update Product from Metadata
    def updateProduct(self, id):
        productdata = self.getProductMetadata(id)
        if productdata != None:
            self.products[int(productdata["id"])] = productdata
            productpath = os.path.join(Product_DataPath, str(productdata["id"]))
            # Remove the photos
            self.removeMedia(productdata["procedure"], productpath)
            return True
        else:
            return False

    # Delete product from Metadata
    def deleteProduct(self, parsedMetadata, productpath):
        #     Delete photos
        self.removeMedia(parsedMetadata["procedure"], productpath)


    # TODO If there are more media files, this needs to change
    # Checks for inconsistencies in the metadata images and the actual images
    # Removes the ones that are not tracked
    def removeMedia(self, procedurelist, productpath):
        # Not to delete
        medialist = []
        if (procedurelist != []):
            for i in range(len(procedurelist)):
                medialist.append(procedurelist[i]["image"])
                if 'video' in procedurelist[i] and procedurelist[i]['video'] != None:
                    medialist.append(procedurelist[i]['video'])

        #     Delete photos
        if len(medialist) > 0:
            delmedia = []
            filelist = os.listdir(productpath)
            for f in filelist:
                if f.endswith(".jpg") and not f in medialist:
                    delmedia.append(f)
                if f.endswith(".mp4") and not f in medialist:
                    delmedia.append(f)

            if len(delmedia) > 0:
                for media in delmedia:
                    os.remove(os.path.join(productpath, media))
        else:
            # Remove all
            filelist = os.listdir(productpath)
            for f in filelist:
                if f.endswith(".jpg") or f.endswith(".mp4"):
                    os.remove(os.path.join(productpath, f))


    # Adding a new or edit productData
    def addProductData(self, product, conn):
        prdctmeta = {}
        newproductpath = os.path.join(Product_DataPath, product["number"])
        # Making the file
        if not os.path.exists(newproductpath):
            os.makedirs(newproductpath)
        # Making the metadata
        newproductmeta = os.path.join(newproductpath, self.metadatafile)
        prdctmeta["id"] = int(product["number"])
        prdctmeta["name"] = product["name"]
        prdctmeta["author"] = product["creator"]
        prdctmeta["created"] = product["date"]
        prdctmeta["modified"] = product["last_date"]
        # TODO Change the product_name in procedure
        if self.checkIDExists(int(product["number"])):
            prdctmeta["procedure"] = self.products[int(product["number"])]["procedure"]
        else:
            prdctmeta["procedure"] = None
        self.products[int(product["number"])] = prdctmeta

        self.makeJSONFile(prdctmeta, newproductmeta)
        # Parsing dates
        createddate = self.makeStringTime(prdctmeta["created"])
        modifieddata = self.makeStringTime(prdctmeta["modified"])

        # Add to category
        category = None
        if "category" in product:
            category = product["category"]
        update = self.db_updateProduct(conn, int(product["number"]), category)
        if update:
            # Update the local storage too
            self.updateLocalCategories(conn)
            return [prdctmeta["id"], prdctmeta["name"], createddate, modifieddata , prdctmeta["author"], category]
        else:
            return None


    # Update the metadata file and the product id
    def updateMetadata(self, parsedMetadata):
        if self.checkProductDataCorrectness(parsedMetadata):
            self.products[parsedMetadata['id']] = parsedMetadata
            #     Write to file
            productpath = os.path.join(Product_DataPath, str(parsedMetadata['id']))
            newmetadatapath = os.path.join(productpath, self.metadatafile)
            if os.path.isdir(productpath):
                self.makeJSONFile(parsedMetadata, newmetadatapath)
            return productpath
        else:
            # The data sent was corrupted
            return None

    # Creates the JSON File to send
    # No more pickle since can't unwrap in android
    def makeJSONFile(self, obj, file):
        with open(file, 'wb') as fp:
            jsonobj = json.dumps(obj)
            fp.write(jsonobj)


    # Making zip files
    def unpackageZipFile(self, filename, filedata):
        # Creating the file from bytes
        foldername = os.path.join(Working_DataPath, filename)
        with open(foldername, "wb") as fh:
            fh.write(filedata)
        #     Update the DB here
        return self.unzipFiles(foldername)

    #     This will unzip the files check the metadata and the ser file and update the folders
    def unzipFiles(self, filename):
        foldername = os.path.join(Working_DataPath, filename)
        try:
            zip_ref = zipfile.ZipFile(foldername, 'r')
            zip_ref.extractall(Working_DataPath)
            zip_ref.close()
            return zip_ref.namelist()
        except:
            return

    def getNumberFiles(self, productid):
        filetozip = os.path.join(Product_DataPath, str(productid))
        if os.path.exists(filetozip):
            return len([name for name in os.listdir(filetozip) if os.path.isfile(os.path.join(filetozip, name))])

    # Make a zipfile of the product file to send to the tablet
    def makeZipFile(self, productid):
        zipf = zipfile.ZipFile(os.path.join(Working_DataPath, str(productid)+'.zip'), 'w', zipfile.ZIP_DEFLATED)
        filetozip = os.path.join(Product_DataPath, str(productid))
        # Check if there is any data
        if os.path.exists(filetozip):
            # Zipping files
            for file in os.listdir(filetozip):
                zipf.write(os.path.join(filetozip, file), file)
            zipf.close()
            return os.path.join(Working_DataPath, str(productid)+'.zip')
        else:
            return None


    # This function will delete all the files in the selected folder
    def deleteAllFiles(self, path):
        if (os.path.exists(path)):
            for file in os.listdir(path):
                file_path = os.path.join(path, file)
                try:
                    if os.path.isfile(file_path):
                        # Same as remove
                        os.unlink(file_path)
                    elif os.path.isdir(file_path):
                        shutil.rmtree(file_path)
                except Exception as e:
                    print(e)


    # Removing the whole file
    # It gets the id
    def deleteProductData(self, productid):
        # Deleting the product data from dictionary
        if self.checkIDExists(int(productid)):
            del self.products[int(productid)]
            delproductpath = os.path.join(Product_DataPath, productid)
            if os.path.exists(delproductpath):
                shutil.rmtree(delproductpath)
                return productid
            else:
                return None
        else:
            return None


    # Functionality function make timestamp into readable string
    def makeStringTime(self, timestamp):
        stringtime = datetime.datetime.fromtimestamp(timestamp/1000).strftime('%Y-%m-%d %H:%M:%S')
        return stringtime


###################################################################################
# Functions to create the excel file to send
####################################################################################

    # This function will create the excel file for download
    def createExcelFile(self, productid):
        # Create workbook and work sheet
        excelfilename = productid + ".xlsx"
        excelpath = os.path.join(PrintFiles_DataPath, excelfilename)
        excelworkbook = xlsxwriter.Workbook(excelpath)
        worksheet = excelworkbook.add_worksheet()
        productmeta = self.getProductMetadata(productid)

        # Write the header
        # Formats
        header_format = excelworkbook.add_format({
            'bold' : 1,
            'align' : 'center',
            'valign' : 'vcenter',
            'border' : 1
        })

        value_format = excelworkbook.add_format({
            'align' : 'center',
            'valign' : 'vcenter',
            'border' : 1
        })

        # B, C ,D Columns headers
        worksheet.merge_range('B2:D2', u'梱包手順書', header_format)
        worksheet.write('B3', u'管理番号', header_format)
        worksheet.merge_range('B4:B5', u'品名', header_format)
        # B, C ,D Columns values
        worksheet.merge_range('C3:D3', productmeta["id"], value_format)
        worksheet.merge_range('C4:D5', productmeta["name"], value_format)

        # E, F Column headers
        worksheet.merge_range('E2:G2', u'更新履歴', header_format)
        worksheet.write('E3',"", value_format)
        worksheet.write('E4',"", value_format)
        worksheet.write('E5',"", value_format)
        worksheet.merge_range('F3:G3',self.makeStringTime(productmeta['modified']), value_format)
        worksheet.merge_range('F4:G4',"", value_format)
        worksheet.merge_range('F5:G5',"", value_format)

        # G, H Column Headers
        worksheet.write('H2', u'承認', header_format)
        worksheet.write('H3', u'', value_format)
        worksheet.write('H4', u'', value_format)
        worksheet.write('H5', u'', value_format)
        worksheet.write('I2', u'作成', header_format)
        # Adding the user here
        worksheet.write('I3', productmeta["author"], value_format)
        worksheet.write('I4', u'', value_format)
        worksheet.write('I5', u'', value_format)

        # I, J , K Columns
        # Needs to add the time
        created_time = u'制定 ' + datetime.datetime.fromtimestamp(productmeta['created']/1000).strftime('%Y/%m/%d')
        worksheet.merge_range('J2:L2', created_time, header_format)
        worksheet.write('J3', u'承認', header_format)
        worksheet.merge_range("J4:J5", "", header_format)
        worksheet.write('K3', u'確認', header_format)
        # Diagonal line format
        diagformat = excelworkbook.add_format({
            'diag_type' : 1,
            'bold': 1,
            'align': 'center',
            'valign': 'vcenter',
            'border': 1
        })
        worksheet.merge_range("K4:K5", "", diagformat)
        worksheet.write('L3', u'作成', header_format)
        worksheet.merge_range("L4:L5", "", header_format)

        # Write the contents here : Start at B7 to L7
        content_format = excelworkbook.add_format({
            'border': 1
        })

        # Make the contents
        self.makeContentForExcel(productmeta, worksheet, content_format)

        excelworkbook.close()

        return excelpath

    # Parse the ser files to make the excel
    # productmeta["procedure"] will be passed here
    def makeContentForExcel(self, product, worksheet, contentformat):
        procedurelist = product["procedure"]
        startindex = 7

        cell_width = 64.0 * 6
        cell_height = 20.0 * 13

        image_format = {
            'x_offset': 5,
            'y_offset': 5,
            'x_scale': 0.2,
            'y_scale': 0.2
        }
        if procedurelist != None:
            for i in range(len(procedurelist)):
                # This is the dictionary with the ser and image file name
                procedure = procedurelist[i]
            #     Add the parsed Ser into the worksheet
            #   Make the index
                index_title = 'B'+ str(startindex) + ':E' + str(startindex + 2)
                title_data = procedure["procedure_title"]
                worksheet.merge_range(index_title, title_data, contentformat)

                #         Get the image file
                imagefilepath = os.path.join(Product_DataPath, str(product['id']), procedure['image'])
                if os.path.exists(imagefilepath):
                    # Calculating ratio for image
                    img = Image.open(imagefilepath)
                    # Calculating image sizes
                    width, height = img.size

                    ratio = min(cell_width/ width,cell_height/height)

                    image_format['x_scale'] = ratio
                    image_format['y_scale'] = ratio

                    indeximage = 'G' + str(startindex)
                    worksheet.insert_image(indeximage, imagefilepath, image_format)
                # Adding the comments
                startindex += 3
                index_text = 'B' + str(startindex ) + ':E' + str(startindex + 9)
                textdata = procedure["procedure_details"]
                worksheet.merge_range(index_text, textdata, contentformat)
                startindex += 11
        else:
            # If not procedurelist exist then pass
            pass


#     The run thread itself
    def run(self):
        log.info("Initializing Server")
        log.info("Starting Procedure")

        log.info("Starting thread")
        while(self.alive == 0):
            # Gets all the data every 100ms
            # Need to create an update method
            # This function monitors the socket until they can be read or written
            if (self.alive):
                continue
            time.sleep(1)

        log.info("Thread Complete")
        log.info("Closing connection")


thread_list = []
def signal_handler(sig, frame):
    for i in thread_list:
        i.stopthread()
    sys.exit(0)


def main():
    signal.signal(signal.SIGINT, signal_handler)
    backend = ClientThread()
    thread_list.append(backend)
    cherrypy.quickstart(WebAPI(backend))      

main()





     
