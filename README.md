


MainActivity.kt: Fetches image URLs from the API and constructs them using the provided JSON structure. The URLs are then added to the list and the adapter is notified.
ImageAdapter.kt: Manages image loading and caching. It uses LruCache for in-memory caching and a local file for disk caching. Images are loaded asynchronously.

Use App Cache Directory: Use the app's cache directory for storing the images.
Disk Caching: Images are saved to and loaded from the app's cache directory, using the image URL's hash code as the file name.
