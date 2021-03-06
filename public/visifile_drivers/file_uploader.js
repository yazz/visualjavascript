async function(records) {
    description("This function is when a file is uploaded to the system")
    on( {
    where: "tags like '%||  UPLOAD  ||%'"
    })

    var promise = new Promise(success => {
    //console.log("1) In File Uploader, calling  a query")
    //console.log("2) " + JSON.stringify(records,null,2))
    var record =   getFirstRecord(records)
    var fullFilePath = getProperty(record,"path")
    //console.log("3) " + JSON.stringify(fullFilePath,null,2))
    //console.log("4) getFileExtension=" + getFileExtension(fullFilePath))
    findDriverWithMethod(   "can_handle_" + getFileExtension(fullFilePath)
                        ,
                        function(driverName) {

                            if (driverName) {
                                //console.log("5) base_component_id:" + driverName)
                                callDriverMethod( driverName,
                                                  "can_handle_" + getFileExtension(fullFilePath),
                                                  {fileName: fullFilePath}
                                            ,
                                            function(result) {
                                                console.log("File uploader 3) returned result: " + JSON.stringify(result,null,2))
                                                 var sha1ofFileContents = createHashedDocumentContent(fullFilePath, getFileExtension(fullFilePath))
                                                saveDocumentContent(sha1ofFileContents,result)
                                                setStatus(record, "SAVED")
                                                addTag(record, "DOCUMENT")
                                                setProperty(record, "hash", sha1ofFileContents)
                                                setHash(record, sha1ofFileContents)
                                                setName(record, getFileName(fullFilePath))
                                                setAddedTimestamp(record, new Date().valueOf())
                                                setEstimatedModifiedTimestamp(record, new Date().valueOf())

                                                success({})
                                            })

                            } else {
                                console.log("5) No base_component_id can handle: " + getFileExtension(fullFilePath))
                                setStatus(record, "ERROR")
                                success({})
                            }
                        })
                    })
                    console.log("File uploader 1)" )
                    var ret = await promise
                    console.log("File uploader 5) returned result: " + JSON.stringify(ret,null,2))
                    return ret
}
