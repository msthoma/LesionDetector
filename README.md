# Lesion detector

A smartphone app built with Flutter, capable of detecting skin cancer lesions using Computer Vision. 
To do so, the app incorporates a Convolutional Neural Network trained with the 
[HAM10000 dataset](https://dataverse.harvard.edu/dataset.xhtml?persistentId=doi:10.7910/DVN/DBW86T), 
which consists of _"a large collection of multi-source dermatoscopic images of common pigmented skin
lesions"_.

## Convolutional Neural Network information

The CNN used was trained using TensorFlow and Python, as explained in detail in this 
[Github repository](https://github.com/msthoma/HAM10000_ConvNet) and this
[Google Colab notebook](https://github.com/msthoma/HAM10000_ConvNet/blob/master/HAM10000_ConvNet.ipynb).

## App information

The app is built with the Flutter framework, and so it works on both Android and iOS. Most of the 
app's functionality is implemented in this file: [/lib/main.dart](/lib/main.dart).