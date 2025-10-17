import os

from fastapi import FastAPI, UploadFile
import uvicorn
app = FastAPI()

workDir = './workTmp/'

@app.get("/test")
def root():
    return 'hello'
@app.post("/fileupload")
async def map_file_upload(txtFile:UploadFile):
    try:
        if not os.path.exists(workDir):
             os.makedirs(workDir)
        with open(workDir + txtFile.filename, 'wb') as f:
            f.write(txtFile.file.read())
        return {'code':200,'msg':'success'}
    except Exception as e:
        return {
            'code':500,
            'msg':str(e)
            }

if __name__ == '__main__':
    uvicorn.run(app,port=8000)